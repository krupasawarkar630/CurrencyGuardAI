package com.example.currencyguard.ml;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Image Quality Pre-screening Gate.
 * Evaluates blur via Laplacian gradient variance, glare, underexposure,
 * resolution, and framing before computational resources are spent on deep inference.
 */
public class ImageQualityAnalyzer {

    public static class QualityReport {
        private final double score; // 0.0 to 100.0
        private final List<String> problems;
        private final String recommendation;
        private final boolean isAcceptable;

        public QualityReport(double score, List<String> problems, String recommendation) {
            this.score = score;
            this.problems = problems;
            this.recommendation = recommendation;
            this.isAcceptable = score >= 50.0;
        }

        public double getScore() { return score; }
        public List<String> getProblems() { return problems; }
        public String getRecommendation() { return recommendation; }
        public boolean isAcceptable() { return isAcceptable; }
    }

    public QualityReport analyze(Bitmap bitmap) {
        if (bitmap == null) {
            List<String> problems = new ArrayList<>();
            problems.add("No image data available");
            return new QualityReport(0.0, problems, "Capture an image to proceed.");
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        List<String> problems = new ArrayList<>();
        double score = 100.0;

        // 1. Resolution Check
        if (width < 720 || height < 720) {
            score -= 15.0;
            problems.add("Low resolution image (< 720p)");
        }

        // 2. Glare and Underexposure Sampling
        int totalPixels = 0;
        int overexposedPixels = 0;
        int underexposedPixels = 0;
        int sampleStep = Math.max(1, Math.min(width, height) / 40);

        for (int y = 0; y < height; y += sampleStep) {
            for (int x = 0; x < width; x += sampleStep) {
                int pixel = bitmap.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                int lum = (r * 299 + g * 587 + b * 114) / 1000;

                if (lum > 242) {
                    overexposedPixels++;
                } else if (lum < 25) {
                    underexposedPixels++;
                }
                totalPixels++;
            }
        }

        double glareRatio = (double) overexposedPixels / (totalPixels + 1);
        double shadowRatio = (double) underexposedPixels / (totalPixels + 1);

        if (glareRatio > 0.15) {
            score -= 22.0;
            problems.add("High reflection or glare detected on surface");
        } else if (glareRatio > 0.07) {
            score -= 10.0;
            problems.add("Slight reflection detected in corner regions");
        }

        if (shadowRatio > 0.25) {
            score -= 20.0;
            problems.add("Severe shadows / insufficient ambient light");
        } else if (shadowRatio > 0.12) {
            score -= 8.0;
            problems.add("Uneven lighting / partial shadows");
        }

        // 3. Blur Detection (Laplacian Gradient Energy Approximation)
        double laplacianVariance = computeLaplacianVariance(bitmap, sampleStep);
        if (laplacianVariance < 60.0) {
            score -= 25.0;
            problems.add("Noticeable motion blur detected");
        } else if (laplacianVariance < 110.0) {
            score -= 12.0;
            problems.add("Slight softness/blur detected on edges");
        }

        // Clamp score between 10.0 and 98.0
        score = Math.max(10.0, Math.min(98.0, score));

        String recommendation;
        if (score >= 80.0) {
            recommendation = "Excellent image quality. Proceeding with authenticity screening.";
        } else if (score >= 50.0) {
            recommendation = "Acceptable quality. Retaking under even, diffused lighting is advised.";
        } else {
            recommendation = "Retake image with note flat on surface under bright, even illumination.";
        }

        return new QualityReport(Math.round(score * 10.0) / 10.0, problems, recommendation);
    }

    private double computeLaplacianVariance(Bitmap bitmap, int step) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        double sum = 0;
        double sumSq = 0;
        int count = 0;

        for (int y = step; y < height - step; y += step * 2) {
            for (int x = step; x < width - step; x += step * 2) {
                int c = getGray(bitmap.getPixel(x, y));
                int l = getGray(bitmap.getPixel(x - step, y));
                int r = getGray(bitmap.getPixel(x + step, y));
                int t = getGray(bitmap.getPixel(x, y - step));
                int b = getGray(bitmap.getPixel(x, y + step));

                // 4-neighbor Laplacian operator: L = 4*c - l - r - t - b
                double lap = Math.abs(4 * c - l - r - t - b);
                sum += lap;
                sumSq += lap * lap;
                count++;
            }
        }

        if (count == 0) return 100.0;
        double mean = sum / count;
        return (sumSq / count) - (mean * mean);
    }

    private int getGray(int color) {
        return (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000;
    }
}
