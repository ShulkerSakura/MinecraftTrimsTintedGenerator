import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class TintedTextureGenerator {

    public static void main(String[] args) {
        System.out.println("Input base texture file name: ");
        Scanner sc = new Scanner(System.in);
        String baseName = sc.next();
        System.out.println("Input color texture file name: ");
        String colorName = sc.next();
        try {
            // 1. 加载基础纹理（必须是带 Alpha 的 PNG）
            BufferedImage baseImg = ImageIO.read(new File(baseName));
            int width = baseImg.getWidth();
            int height = baseImg.getHeight();

            // 2. 加载调色板（1×8 像素）
            BufferedImage paletteImg = ImageIO.read(new File(colorName));
            if (paletteImg.getWidth() != 8 || paletteImg.getHeight() != 1) {
                throw new IllegalArgumentException("调色板必须是 1×8 像素的图像");
            }

            // 3. 提取调色板颜色（从左到右：亮 → 暗）
            Color[] palette = new Color[8];
            for (int i = 0; i < 8; i++) {
                int rgb = paletteImg.getRGB(i, 0);
                palette[i] = new Color(rgb);
            }

            // 4. 创建输出图像（RGBA）
            BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            // 5. 遍历每个像素
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = baseImg.getRGB(x, y);

                    // 提取原始 Alpha（非常重要！）
                    int alpha = (argb >> 24) & 0xFF;

                    // 如果原始是完全透明，直接设为透明，跳过着色
                    if (alpha == 0) {
                        output.setRGB(x, y, 0x00000000); // 完全透明
                        continue;
                    }

                    // 提取 RGB 分量（用于计算亮度）
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;

                    // 使用标准亮度公式（ITU-R BT.601）
                    double grayValue = 0.299 * r + 0.587 * g + 0.114 * b;

                    // 映射到调色板索引：0=最亮（左），7=最暗（右）
                    double normalized = grayValue / 255.0;
                    double floatIndex = 7 * (1 - normalized); // 0.0 ~ 7.0

                    int idxLow = (int) Math.floor(floatIndex);
                    int idxHigh = Math.min(idxLow + 1, 7);
                    double weight = floatIndex - idxLow;

                    Color lowColor = palette[idxLow];
                    Color highColor = palette[idxHigh];

                    // 插值生成新颜色
                    int rFinal = (int)(lowColor.getRed() * (1 - weight) + highColor.getRed() * weight);
                    int gFinal = (int)(lowColor.getGreen() * (1 - weight) + highColor.getGreen() * weight);
                    int bFinal = (int)(lowColor.getBlue() * (1 - weight) + highColor.getBlue() * weight);

                    // ✅ 关键：使用原始 Alpha，不修改它！
                    // 灰度只影响颜色，不影响透明度
                    int finalARGB = (alpha << 24) | (rFinal << 16) | (gFinal << 8) | bFinal;
                    output.setRGB(x, y, finalARGB);
                }
            }

            // 6. 保存结果
            File outputFile = new File("final_tinted_output.png");
            ImageIO.write(output, "PNG", outputFile);
            System.out.println("✅ 成功生成着色纹理：final_tinted_output.png");

        } catch (IOException e) {
            System.err.println("❌ 错误：无法读取或写入文件。请确保 'base.png' 和 'color_palettes.png' 存在于当前目录。");
            e.printStackTrace();
        }
    }
}