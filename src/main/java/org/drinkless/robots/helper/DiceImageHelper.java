package org.drinkless.robots.helper;

import cn.hutool.core.util.RandomUtil;
import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class DiceImageHelper {
    private static final int CELL_SIZE = 50;
    private static final int ROWS = 6;
    private static final int COLS = 14;
    private static final int WIDTH = COLS * CELL_SIZE;
    private static final int STATS_HEIGHT = 40;
    private static final int HEIGHT = (ROWS * CELL_SIZE) + STATS_HEIGHT;
    private static final int CIRCLE_PADDING = 5;

    public static void main(String[] args) {
        List<Integer> diceNumbers = new java.util.ArrayList<>();
        for (int i = 0; i < (14 * 6) - 1; i++) {
            diceNumbers.add(RandomUtil.randomInt(3, 18));
        }
        generateDiceImage(diceNumbers, "dice.png");
    }
    
    /**
     * 生成骰子统计图片
     * @param diceNumbers 骰子点数列表
     * @param outputPath 输出图片路径
     */
    public static void generateDiceImage(List<Integer> diceNumbers, String outputPath) {
        DiceStatistics stats = new DiceStatistics();
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        
        // 添加所有点数
        for (Integer number : diceNumbers) {
            stats.addDiceSum(number);
        }
        
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // 重新填充白色背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        // 绘制网格和圆形
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int x = col * CELL_SIZE;
                int y = row * CELL_SIZE;
                
                // 绘制单元格边框
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.drawRect(x, y, CELL_SIZE, CELL_SIZE);

                // 获取并绘制数值
                int value = stats.getValue(row, col);
                if (value > 0) {
                    // 绘制圆形背景
                    g2d.setColor(stats.getColor(value));
                    int circleSize = CELL_SIZE - (CIRCLE_PADDING * 2);
                    g2d.fillOval(x + CIRCLE_PADDING, y + CIRCLE_PADDING, 
                                circleSize, circleSize);
                    
                    // 绘制数字
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.BOLD, 20));
                    String text = String.valueOf(value);
                    FontMetrics fm = g2d.getFontMetrics();
                    int textX = x + (CELL_SIZE - fm.stringWidth(text)) / 2;
                    int textY = y + ((CELL_SIZE + fm.getAscent()) / 2);
                    g2d.drawString(text, textX, textY);
                }
            }
        }

        // 统计区域Y坐标
        int statsY = ROWS * CELL_SIZE + 25;
        
        // 设置统一字体
        Font statsFont = new Font("Microsoft YaHei", Font.BOLD, 16);
        g2d.setFont(statsFont);
        
        // 绘制"统计"文字
        g2d.setColor(Color.BLACK);
        g2d.drawString("统计", 10, statsY);
        
        // 计算方形大小
        int squareSize = 24;
        
        // 绘制03统计
        int firstSquareX = 60;
        g2d.setColor(new Color(93, 162, 47));
        g2d.fillRect(firstSquareX, statsY - 20, squareSize, squareSize);
        
        // 绘制03文字
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics fm = g2d.getFontMetrics();
        String text03 = "03";
        int text03X = firstSquareX + (squareSize - fm.stringWidth(text03)) / 2;
        g2d.drawString(text03, text03X, statsY - 5);
        
        // 绘制03计数
        g2d.setColor(Color.BLACK);
        g2d.setFont(statsFont);
        g2d.drawString(String.valueOf(stats.getCount03()), firstSquareX + squareSize + 5, statsY);
        
        // 绘制18统计
        int secondSquareX = firstSquareX + squareSize + 40;
        g2d.setColor(new Color(93, 162, 47));
        g2d.fillRect(secondSquareX, statsY - 20, squareSize, squareSize);
        
        // 绘制18文字
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String text18 = "18";
        int text18X = secondSquareX + (squareSize - fm.stringWidth(text18)) / 2;
        g2d.drawString(text18, text18X, statsY - 5);
        
        // 绘制18计数
        g2d.setColor(Color.BLACK);
        g2d.setFont(statsFont);
        g2d.drawString(String.valueOf(stats.getCount18()), secondSquareX + squareSize + 5, statsY);

        try {
            ImageIO.write(image, "PNG", new File(outputPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        g2d.dispose();
    }
    
    private static class DiceStatistics {
        private final int[][] grid = new int[ROWS][COLS];
        private int currentRow = 0;
        @Getter
        private int count03 = 0;
        @Getter
        private int count18 = 0;

        public void addDiceSum(int sum) {
            if (currentRow >= ROWS) {
                shiftLeft();
                currentRow = 0;
            }
            grid[currentRow][COLS-1] = sum;
            if (sum == 3) count03++;
            if (sum == 18) count18++;
            currentRow++;
        }

        private void shiftLeft() {
            for (int i = 0; i < ROWS; i++) {
                for (int j = 0; j < COLS-1; j++) {
                    grid[i][j] = grid[i][j+1];
                }
            }
            for (int i = 0; i < ROWS; i++) {
                grid[i][COLS-1] = 0;
            }
        }

        public Color getColor(int sum) {
            if (sum == 3 || sum == 18) {
                return new Color(93, 162, 47);  // 绿色
            } else if (sum >= 3 && sum <= 10) {
                return new Color(76, 82, 211);  // 蓝色
            } else if (sum >= 11 && sum <= 18) {
                return new Color(210, 67, 66);  // 红色
            }
            return Color.WHITE;
        }

        public int getValue(int row, int col) {
            return grid[row][col];
        }

    }
}