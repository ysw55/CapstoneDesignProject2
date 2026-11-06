package com.bookcanvas.domain.book.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 표지 전체에서 대표 색 3가지를 뽑는 로직 (이미지를 직접 그려서 검증) */
class BookCoverPaletteTest {

    private final BookCoverColorExtractorService extractor = new BookCoverColorExtractorService();

    /** 64 x 64 이미지를 위에서부터 [색, 비율(%)] 순서로 가로 띠로 채운다 */
    private BufferedImage bands(int[][] colorAndPercent) {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        int y = 0;
        for (int[] cp : colorAndPercent) {
            int rows = Math.round(64 * cp[1] / 100f);
            for (int row = 0; row < rows && y < 64; row++, y++) {
                for (int x = 0; x < 64; x++) image.setRGB(x, y, cp[0]);
            }
        }
        for (; y < 64; y++) for (int x = 0; x < 64; x++) image.setRGB(x, y, colorAndPercent[colorAndPercent.length - 1][0]);
        return image;
    }

    @Test
    @DisplayName("대표 색을 첫 번째로, 나머지는 많이 나온 순으로 서로 다른 색 3가지")
    void threeDistinctColorsInOrder() {
        BufferedImage image = bands(new int[][]{{0x102040, 60}, {0xF0E8D0, 25}, {0xC03020, 15}});

        List<String> colors = extractor.topColors(image, 0x102040);

        assertThat(colors).containsExactly("#102040", "#F0E8D0", "#C03020");
    }

    @Test
    @DisplayName("흰 배경이 가장 많아도 첫 색은 넘겨 받은 대표 색, 흰색은 그다음 색으로 들어간다")
    void primaryFirstEvenIfNotMostFrequent() {
        BufferedImage image = bands(new int[][]{{0xFFFFFF, 70}, {0x2050C0, 20}, {0xC03020, 10}});

        List<String> colors = extractor.topColors(image, 0x2050C0);

        assertThat(colors).containsExactly("#2050C0", "#FFFFFF", "#C03020");
    }

    @Test
    @DisplayName("비슷한 색은 하나로 보고 건너뛴다")
    void skipsSimilarColors() {
        // 남색과 거의 같은 남색은 건너뛰고, 서로 다른 크림·빨강이 들어간다
        BufferedImage image = bands(new int[][]{{0x102040, 40}, {0x182848, 30}, {0xF0E8D0, 20}, {0xC03020, 10}});

        List<String> colors = extractor.topColors(image, 0x102040);

        assertThat(colors).containsExactly("#102040", "#F0E8D0", "#C03020");
    }

    @Test
    @DisplayName("색이 한두 가지뿐인 표지는 있는 만큼만")
    void fewerThanThree() {
        BufferedImage image = bands(new int[][]{{0x336699, 100}});

        assertThat(extractor.topColors(image, 0x336699)).containsExactly("#336699");
    }

    @Test
    @DisplayName("색이 많아도 최대 3개")
    void atMostThree() {
        BufferedImage image = bands(new int[][]{{0xFF0000, 20}, {0x00FF00, 20}, {0x0000FF, 20}, {0xFFFF00, 20}, {0xFF00FF, 20}});

        assertThat(extractor.topColors(image, 0xFF0000)).hasSize(3);
    }
}
