package com.bookcanvas.domain.book.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 도서 표지 이미지에서 주요 색상(Primary Color)을 추출하는 서비스
 *
 * <p><b>알고리즘:</b> 색상 양자화(Color Quantization) 기반 빈도수 분석
 * <ol>
 *   <li>이미지 URL에서 {@link BufferedImage}를 다운로드</li>
 *   <li>성능을 위해 이미지를 썸네일 사이즈(64x64)로 리사이즈</li>
 *   <li>각 픽셀의 RGB 값을 4비트 단위로 양자화하여 유사 색상을 그룹핑</li>
 *   <li>빈도수가 가장 높은 색상 버킷을 추출</li>
 *   <li>결과를 Hex Code 형태(#RRGGBB)로 변환하여 반환</li>
 * </ol>
 *
 * <p><b>양자화 이유:</b> 이미지는 픽셀마다 미세하게 다른 색상 값을 가집니다.
 * 양자화 없이 단순 빈도를 세면 정확히 일치하는 픽셀이 거의 없어
 * 올바른 "주요 색상"을 뽑기 어렵습니다.
 * 각 채널을 4비트(16단계)로 줄이면 유사 색상들이 동일 버킷으로 묶입니다.
 */
@Slf4j
@Service
public class BookCoverColorExtractorService {

    /** 리사이즈할 썸네일 크기 (픽셀). 크기가 클수록 정확하지만 처리 시간이 증가합니다. */
    private static final int SAMPLE_SIZE = 64;

    /**
     * 색상 양자화 비트 수 (채널당).
     * 4 → 각 채널을 상위 4비트만 사용 (256단계 → 16단계 축소)
     * 값이 작을수록 더 넓은 범위의 색상을 같은 그룹으로 묶습니다.
     */
    private static final int QUANTIZE_BITS = 4;

    /** 배경색 판별 임계값. 이 비율 이상이면 배경 색상으로 간주하여 제외합니다. */
    private static final double BACKGROUND_THRESHOLD_RATIO = 0.5;

    /**
     * 이미지 URL에서 주요 색상을 추출하여 Hex Code로 반환합니다.
     *
     * @param imageUrl 도서 표지 이미지 URL (HTTP/HTTPS)
     * @return Hex Code 형태의 주요 색상 (예: "#A95BCD")
     * @throws BookCoverColorExtractorService.ColorExtractionException 이미지 로딩 또는 색상 추출 실패 시
     */
    public String extractPrimaryColorHex(String imageUrl) {
        log.info("[ColorExtractor] 색상 추출 시작 - URL: {}", imageUrl);

        BufferedImage originalImage = loadImage(imageUrl);
        BufferedImage resizedImage = resizeImage(originalImage, SAMPLE_SIZE, SAMPLE_SIZE);

        int dominantArgb = findDominantColor(resizedImage);
        String hexColor = toHexCode(dominantArgb);

        log.info("[ColorExtractor] 색상 추출 완료 - 결과: {}", hexColor);
        return hexColor;
    }

    /**
     * 주요 색상보다 어두운 보조 색상(테두리용)을 추출합니다.
     * 배경색으로 밝은 주요 색상을 쓸 때 테두리를 짙게 처리하는 데 활용합니다.
     *
     * @param primaryColorHex 주요 색상 Hex Code
     * @return 주요 색상을 30% 어둡게 처리한 Hex Code
     */
    public String deriveBorderColor(String primaryColorHex) {
        Color primary = Color.decode(primaryColorHex);
        float[] hsb = Color.RGBtoHSB(primary.getRed(), primary.getGreen(), primary.getBlue(), null);

        // Brightness를 30% 낮추어 어두운 보더 색상 생성
        float darkerBrightness = Math.max(0f, hsb[2] - 0.30f);
        int darkerRgb = Color.HSBtoRGB(hsb[0], hsb[1], darkerBrightness);

        return toHexCode(darkerRgb);
    }

    // =========================================================================
    //  Private 헬퍼 메서드
    // =========================================================================

    /**
     * URL에서 이미지를 다운로드합니다.
     * Java의 기본 {@link ImageIO}를 사용하며, 외부 라이브러리가 필요 없습니다.
     */
    private BufferedImage loadImage(String imageUrl) {
        try {
            URL url = URI.create(imageUrl).toURL();
            // ImageIO.read()는 JPEG, PNG, GIF, BMP, WBMP 포맷을 기본 지원합니다.
            BufferedImage image = ImageIO.read(url);
            if (image == null) {
                throw new ColorExtractionException("이미지를 읽을 수 없습니다. URL을 확인하거나 지원하는 포맷(JPEG, PNG, GIF)인지 확인하세요: " + imageUrl);
            }
            return image;
        } catch (IOException e) {
            log.error("[ColorExtractor] 이미지 로딩 실패 - URL: {}, 원인: {}", imageUrl, e.getMessage());
            throw new ColorExtractionException("이미지 다운로드 중 오류가 발생했습니다: " + imageUrl, e);
        }
    }

    /**
     * 픽셀 분석 성능 향상을 위해 이미지를 지정한 크기로 리사이즈합니다.
     * {@link Image#SCALE_FAST} 옵션으로 빠른 처리를 우선합니다.
     */
    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(original.getScaledInstance(width, height, Image.SCALE_FAST), 0, 0, null);
        } finally {
            g2d.dispose(); // Graphics 리소스 반드시 해제
        }
        return resized;
    }

    /**
     * 이미지에서 가장 빈번하게 등장하는 색상(버킷)을 찾습니다.
     *
     * <p>색상 양자화 과정:
     * <pre>
     *   원본 R: 0xA9 (10101001) → 마스킹 후: 0xA0 (10100000)
     *   원본 G: 0x5B (01011011) → 마스킹 후: 0x50 (01010000)
     *   원본 B: 0xCD (11001101) → 마스킹 후: 0xC0 (11000000)
     * </pre>
     * 상위 4비트만 남기므로 유사한 색상들이 같은 버킷으로 묶입니다.
     */
    private int findDominantColor(BufferedImage image) {
        Map<Integer, Integer> colorFrequency = new HashMap<>();
        int mask = 0xFF & (0xFF << QUANTIZE_BITS); // 0xF0 — 상위 4비트 마스크

        int totalPixels = image.getWidth() * image.getHeight();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);

                // 완전 투명 픽셀(알파=0) 제외
                int alpha = (argb >> 24) & 0xFF;
                if (alpha == 0) continue;

                // 채널별 양자화 (하위 QUANTIZE_BITS 비트를 0으로 마스킹)
                int r = ((argb >> 16) & 0xFF) & mask;
                int g = ((argb >> 8)  & 0xFF) & mask;
                int b = (argb         & 0xFF) & mask;

                int quantizedColor = (r << 16) | (g << 8) | b;
                colorFrequency.merge(quantizedColor, 1, Integer::sum);
            }
        }

        if (colorFrequency.isEmpty()) {
            throw new ColorExtractionException("이미지에서 유효한 픽셀을 찾을 수 없습니다.");
        }

        // 배경색(흰색/검정색에 가까운 무채색)이 압도적인 경우 제외하고 차순위 색상 선택
        return selectMeaningfulColor(colorFrequency, totalPixels)
                .orElseGet(() -> findMaxFrequencyColor(colorFrequency)); // 대체 불가 시 최다빈도 색상 사용
    }

    /**
     * 흰색/검정 등 무채색 배경이 지배적인 경우, 이를 제외하고 의미있는 색상을 선택합니다.
     *
     * <p>무채색 판별 기준: R ≈ G ≈ B (채도가 매우 낮음)
     */
    private Optional<Integer> selectMeaningfulColor(Map<Integer, Integer> colorFrequency, int totalPixels) {
        int dominantColor = findMaxFrequencyColor(colorFrequency);
        int dominantCount = colorFrequency.get(dominantColor);

        boolean isDominantAchromatic = isAchromatic(dominantColor);
        boolean isDominantBackground = (double) dominantCount / totalPixels > BACKGROUND_THRESHOLD_RATIO;

        if (isDominantAchromatic && isDominantBackground) {
            log.debug("[ColorExtractor] 무채색 배경 감지, 차순위 색상으로 대체합니다.");
            return colorFrequency.entrySet().stream()
                    .filter(e -> !isAchromatic(e.getKey()))
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey);
        }

        return Optional.of(dominantColor);
    }

    /** 주어진 색상이 무채색(흰색, 검정, 회색 계열)인지 판별합니다. */
    private boolean isAchromatic(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        // 채도 판별: RGB 채널 간 최대 차이가 30 이하이면 무채색으로 간주
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        return (max - min) <= 30;
    }

    /** Map에서 value(빈도)가 가장 높은 key(색상)를 반환합니다. */
    private int findMaxFrequencyColor(Map<Integer, Integer> colorFrequency) {
        return colorFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new ColorExtractionException("색상 빈도 맵이 비어 있습니다."));
    }

    /**
     * RGB 정수 값을 "#RRGGBB" 형태의 Hex Code 문자열로 변환합니다.
     *
     * <p>예: {@code 0xA95BCD} → {@code "#A95BCD"}
     * {@code String.format}의 {@code %06X}는 최소 6자리 대문자 16진수를 보장합니다.
     * (R=0x09 같이 1자리인 경우 앞에 0이 패딩됩니다)
     */
    private String toHexCode(int rgb) {
        // 0x00FFFFFF 마스크로 알파 채널 비트를 제거
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    // =========================================================================
    //  커스텀 예외
    // =========================================================================

    /**
     * 색상 추출 실패 시 던지는 비검사 예외(RuntimeException).
     * 호출부({@code BookCompletionService})에서 사용자에게 적절한 에러 메시지를 반환하도록 처리합니다.
     */
    public static class ColorExtractionException extends RuntimeException {
        public ColorExtractionException(String message) {
            super(message);
        }
        public ColorExtractionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
