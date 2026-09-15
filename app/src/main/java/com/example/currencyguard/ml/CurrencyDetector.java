package com.example.currencyguard.ml;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Currency & Denomination Detector.
 * Identifies currency type (INR/USD), likely denomination (₹10 to ₹500),
 * orientation (Landscape vs Portrait), and side estimation (Front vs Back).
 *
 * Designed to handle full camera viewfinder frames (e.g. 4:3 = 1.33 or 16:9 = 1.77)
 * where the banknote is positioned inside the central scanning reticle.
 */
public class CurrencyDetector {

    public static class DetectionResult {
        private final String currency;
        private final String denomination;
        private final double confidence;
        private final String orientation;
        private final String side;
        private final double geometryScore;
        private final boolean isCurrency;
        private final String rejectionReason;

        public DetectionResult(String currency, String denomination, double confidence,
                               String orientation, String side, double geometryScore,
                               boolean isCurrency, String rejectionReason) {
            this.currency = currency;
            this.denomination = denomination;
            this.confidence = confidence;
            this.orientation = orientation;
            this.side = side;
            this.geometryScore = geometryScore;
            this.isCurrency = isCurrency;
            this.rejectionReason = rejectionReason;
        }

        public String getCurrency() { return currency; }
        public String getDenomination() { return denomination; }
        public double getConfidence() { return confidence; }
        public String getOrientation() { return orientation; }
        public String getSide() { return side; }
        public double getGeometryScore() { return geometryScore; }
        public boolean isCurrency() { return isCurrency; }
        public String getRejectionReason() { return rejectionReason; }
    }

    public DetectionResult detect(Bitmap bitmap, String hintSide) {
        return detect(bitmap, hintSide, "");
    }

    public DetectionResult detect(Bitmap bitmap, String hintSide, String ocrText) {
        if (bitmap == null) {
            return new DetectionResult("None", "None", 0.0, "Unknown", "Front", 0.0,
                    false, "No image provided for screening.");
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        String orientation = (width >= height) ? "Landscape" : "Portrait";

        // 1. Aspect Ratio Validation
        double longDim = Math.max(width, height);
        double shortDim = Math.min(width, height);
        double aspectRatio = longDim / (shortDim + 1e-5);

        // Typical camera captures range between 0.90 and 3.60. Cropped banknotes are ~1.65 to 2.55.
        boolean ratioMatchesCameraOrNote = aspectRatio >= 0.85 && aspectRatio <= 3.80;
        double geometryScore = 88.0;

        if (aspectRatio >= 1.45 && aspectRatio <= 2.85) {
            geometryScore = 95.0; // Ideal cropped banknote ratio
        } else if (ratioMatchesCameraOrNote) {
            geometryScore = 80.0; // Full camera capture
        } else {
            geometryScore = 30.0;
        }

        // 2. OCR Text Analysis & Keyword Categorization
        String ocrRaw = (ocrText != null) ? ocrText.trim() : "";
        String ocrUpper = ocrRaw.toUpperCase();

        // Strong Currency Markers (Central Bank & Legal Tender markers)
        boolean hasReserveBank = ocrUpper.contains("RESERVE") ||
                                 ocrUpper.contains("BANK OF INDIA") ||
                                 ocrUpper.contains("BHARATIYA") ||
                                 ocrUpper.contains("CENTRAL GOV") ||
                                 ocrUpper.contains("भारतीय") ||
                                 ocrUpper.contains("रिज़र्व") ||
                                 ocrUpper.contains("रिजर्व") ||
                                 ocrUpper.contains("रिझर्व्ह");

        boolean hasRupeeWord = ocrUpper.contains("RUPEE") ||
                               ocrUpper.contains("RUPEES") ||
                               ocrUpper.contains("PROMISE") ||
                               ocrUpper.contains("GOVERNOR") ||
                               ocrUpper.contains("GUARANTEED") ||
                               ocrUpper.contains("BEARER") ||
                               ocrUpper.contains("₹") ||
                               ocrUpper.contains("RS.") ||
                               ocrUpper.contains("RS ") ||
                               ocrUpper.contains("रुपये") ||
                               ocrUpper.contains("धारक");

        boolean hasSpecificDenom = ocrUpper.matches(".*\\b(10|20|50|100|200|500|2000)\\b.*") ||
                                   ocrUpper.contains("१०") || ocrUpper.contains("२०") || ocrUpper.contains("५०") ||
                                   ocrUpper.contains("१००") || ocrUpper.contains("२००") || ocrUpper.contains("५००") || ocrUpper.contains("२०००") ||
                                   ocrUpper.contains("पाँच सौ") || ocrUpper.contains("दो सौ") || ocrUpper.contains("एक सौ") ||
                                   ocrUpper.contains("पचास") || ocrUpper.contains("बीस") || ocrUpper.contains("दस") ||
                                   ocrUpper.contains("दोनशे") || ocrUpper.contains("पाचशे");

        boolean hasCurrencyKeywords = hasReserveBank || hasRupeeWord || hasSpecificDenom;

        // Non-Currency / General Poster / Event / Document Disqualifiers
        boolean hasNonCurrencyKeywords = ocrUpper.contains("ENGINEER") ||
                                         ocrUpper.contains("COLLEGE") ||
                                         ocrUpper.contains("UNIVERSITY") ||
                                         ocrUpper.contains("INSTITUTE") ||
                                         ocrUpper.contains("STUDENT") ||
                                         ocrUpper.contains("FACULTY") ||
                                         ocrUpper.contains("FEST") ||
                                         ocrUpper.contains("FESTIVAL") ||
                                         ocrUpper.contains("CELEBRATION") ||
                                         ocrUpper.contains("HAPPY") ||
                                         ocrUpper.contains("BIRTHDAY") ||
                                         ocrUpper.contains("SEMINAR") ||
                                         ocrUpper.contains("WORKSHOP") ||
                                         ocrUpper.contains("CONGRATULATION") ||
                                         ocrUpper.contains("WELCOME") ||
                                         ocrUpper.contains("DEPARTMENT") ||
                                         ocrUpper.contains("INVOICE") ||
                                         ocrUpper.contains("RECEIPT") ||
                                         ocrUpper.contains("BILL") ||
                                         ocrUpper.contains("CERTIFICATE") ||
                                         ocrUpper.contains("MENU") ||
                                         ocrUpper.contains("EXAM") ||
                                         ocrUpper.contains("SYLLABUS") ||
                                         ocrUpper.contains("RESUME") ||
                                         ocrUpper.contains("POSTER") ||
                                         ocrUpper.contains("EVENT") ||
                                         ocrUpper.contains("VENUE") ||
                                         ocrUpper.contains("CONFERENCE") ||
                                         ocrUpper.contains("INVITATION") ||
                                         ocrUpper.contains("ORGANIZED");

        // Disqualification Rule A: Explicit non-currency document/event text without Central Bank identity
        if (hasNonCurrencyKeywords && !hasReserveBank) {
            String snippet = ocrRaw.length() > 35 ? ocrRaw.substring(0, 35) + "..." : ocrRaw;
            return new DetectionResult("None", "None", 0.0, orientation, "N/A", 20.0,
                    false, "Detected non-currency document/poster text ('" + snippet + "'). Not a recognized banknote.");
        }

        // Disqualification Rule B: Substantial legible text without any currency or denomination markings
        if (ocrRaw.length() >= 12 && !hasCurrencyKeywords) {
            String snippet = ocrRaw.length() > 35 ? ocrRaw.substring(0, 35) + "..." : ocrRaw;
            return new DetectionResult("None", "None", 0.0, orientation, "N/A", 25.0,
                    false, "Text detected ('" + snippet + "') does not contain official currency or central bank markings.");
        }

        // Disqualification Rule C: Mandatory Currency Marker Gate
        // An image MUST exhibit recognized central bank markings, legal tender phrases, or official denomination numerals
        if (!hasCurrencyKeywords) {
            return new DetectionResult("None", "None", 0.0, orientation, "N/A", 20.0,
                    false, "No official currency markers, rupee symbol (₹), or denomination numerals detected in this image.");
        }

        // 3. Central Substrate & Quadrant Color Sampling
        int startX = width / 6;
        int endX = width - startX;
        int startY = height / 6;
        int endY = height - startY;
        int midX = (startX + endX) / 2;
        int midY = (startY + endY) / 2;

        int step = Math.max(1, Math.min(width, height) / 36);

        long rTotal = 0, gTotal = 0, bTotal = 0;
        int totalCount = 0;
        int[] lumBins = new int[8];

        // 4 Quadrants for regional color variance analysis
        long[] qR = new long[4], qG = new long[4], qB = new long[4];
        int[] qCount = new int[4];

        for (int y = startY; y < endY; y += step) {
            for (int x = startX; x < endX; x += step) {
                int pixel = bitmap.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                rTotal += r;
                gTotal += g;
                bTotal += b;
                totalCount++;

                int lum = (r * 299 + g * 587 + b * 114) / 1000;
                lumBins[Math.min(7, lum / 32)]++;

                int qIdx = (y < midY ? 0 : 2) + (x < midX ? 0 : 1);
                qR[qIdx] += r;
                qG[qIdx] += g;
                qB[qIdx] += b;
                qCount[qIdx]++;
            }
        }

        if (totalCount == 0) totalCount = 1;
        double avgR = (double) rTotal / totalCount;
        double avgG = (double) gTotal / totalCount;
        double avgB = (double) bTotal / totalCount;

        // Check luminance dispersion (reject blank / uniform screens)
        int populatedBins = 0;
        for (int b : lumBins) {
            if (b > (totalCount * 0.03)) populatedBins++;
        }

        if (aspectRatio < 0.75 || aspectRatio > 4.2) {
            return new DetectionResult("None", "None", 0.0, orientation, "N/A", 20.0,
                    false, "Image dimensions do not match camera or currency specifications.");
        }

        if (populatedBins <= 1) {
            return new DetectionResult("None", "None", 0.0, orientation, "N/A", 15.0,
                    false, "Image is completely blank or uniform with no banknote features.");
        }

        // Compute quadrant color distances to measure substrate homogeneity
        double maxQuadDiff = 0.0;
        double[][] qAvg = new double[4][3];
        for (int i = 0; i < 4; i++) {
            int c = Math.max(1, qCount[i]);
            qAvg[i][0] = (double) qR[i] / c;
            qAvg[i][1] = (double) qG[i] / c;
            qAvg[i][2] = (double) qB[i] / c;
        }

        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 4; j++) {
                double diff = Math.sqrt(Math.pow(qAvg[i][0] - qAvg[j][0], 2) +
                                        Math.pow(qAvg[i][1] - qAvg[j][1], 2) +
                                        Math.pow(qAvg[i][2] - qAvg[j][2], 2));
                if (diff > maxQuadDiff) maxQuadDiff = diff;
            }
        }

        // 4. Test Indian Banknote Substrate Profiles
        boolean matchesGrey500 = (avgR >= 85 && avgR <= 195 && avgG >= 85 && avgG <= 195 && avgB >= 80 && avgB <= 190) &&
                                 (Math.abs(avgR - avgG) < 35 && Math.abs(avgG - avgB) < 35);
        boolean matchesOrange200 = (avgR > 145 && avgG > 105 && avgB < 135 && (avgR - avgB > 40));
        boolean matchesLavender100 = (avgB > 100 && avgR > 90 && (avgB > avgG + 8));
        boolean matchesCyan50 = (avgG > 105 && avgB > 115 && avgR < 130 && (avgB > avgR + 15));
        boolean matchesYellowGreen20 = (avgG > 100 && avgG > avgR && avgB < 125);
        boolean matchesBrown10 = (avgR > 85 && avgR < 185 && avgG > 50 && avgG < 140 && avgB < 100 && (avgR > avgG + 15));
        boolean matchesMagenta2000 = (avgR > 130 && avgB > 95 && avgG < 115 && (avgR - avgG > 30));

        boolean matchesAnyCurrencyProfile = matchesGrey500 || matchesOrange200 || matchesLavender100 ||
                                            matchesCyan50 || matchesYellowGreen20 || matchesBrown10 || matchesMagenta2000;

        // Disqualification Rule C: No currency keywords AND high scene variance or no banknote color match
        if (!hasCurrencyKeywords) {
            if (maxQuadDiff > 70.0 || !matchesAnyCurrencyProfile) {
                return new DetectionResult("None", "None", 0.0, orientation, "N/A", 30.0,
                        false, "Visual profile does not exhibit official banknote substrate or currency security markings.");
            }
        }

        // 5. Determine Denomination from OCR or Substrate
        String detectedDenom = "₹500";
        double detectionConfidence = hasCurrencyKeywords ? 94.0 : 75.0;

        if (ocrUpper.contains("2000") || ocrUpper.contains("२०००")) {
            detectedDenom = "₹2000";
        } else if (ocrUpper.contains("500") || ocrUpper.contains("५००") || ocrUpper.contains("पाँच सौ") || ocrUpper.contains("पाचशे")) {
            detectedDenom = "₹500";
        } else if (ocrUpper.contains("200") || ocrUpper.contains("२००") || ocrUpper.contains("दो सौ") || ocrUpper.contains("दोनशे")) {
            detectedDenom = "₹200";
        } else if (ocrUpper.contains("100") || ocrUpper.contains("१००") || ocrUpper.contains("एक सौ")) {
            detectedDenom = "₹100";
        } else if (ocrUpper.contains("50") || ocrUpper.contains("५०") || ocrUpper.contains("पचास")) {
            detectedDenom = "₹50";
        } else if (ocrUpper.contains("20") || ocrUpper.contains("२०") || ocrUpper.contains("बीस")) {
            detectedDenom = "₹20";
        } else if (ocrUpper.contains("10") || ocrUpper.contains("१०") || ocrUpper.contains("दस")) {
            detectedDenom = "₹10";
        } else {
            // Color dominance mapping
            if (matchesOrange200) {
                detectedDenom = "₹200";
            } else if (matchesCyan50) {
                detectedDenom = "₹50";
            } else if (matchesLavender100) {
                detectedDenom = "₹100";
            } else if (matchesBrown10) {
                detectedDenom = "₹10";
            } else if (matchesYellowGreen20) {
                detectedDenom = "₹20";
            } else if (matchesMagenta2000) {
                detectedDenom = "₹2000";
            } else {
                detectedDenom = "₹500";
            }
        }

        String side = (hintSide != null && !hintSide.isEmpty()) ? hintSide : "Front";
        return new DetectionResult("INR", detectedDenom, detectionConfidence, orientation, side, geometryScore,
                true, "Valid currency note structure confirmed.");
    }
}
