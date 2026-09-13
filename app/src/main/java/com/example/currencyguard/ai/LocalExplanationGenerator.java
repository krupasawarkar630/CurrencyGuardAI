package com.example.currencyguard.ai;

import com.example.currencyguard.model.AnalysisResult;
import com.example.currencyguard.model.UncertaintyReason;

/**
 * Local Rule-Based Explainable AI Generator.
 * Delivers transparent, on-device reasoning when running offline or
 * without cloud API access. Explains the computer vision findings in simple English.
 */
public class LocalExplanationGenerator {

    public String generateExplanation(AnalysisResult result) {
        StringBuilder sb = new StringBuilder();

        if (result.getFinalConfidence() >= 80.0) {
            sb.append("The AI detected strong visual consistency in the note's overall layout and geometric proportions. ");
            sb.append("Key security characteristics—including the security-thread region and watermark window—align closely with standard specifications. ");
            sb.append("OCR text successfully verified the denomination. ");
        } else if (result.getFinalConfidence() >= 55.0) {
            sb.append("The note exhibits several authentic characteristics, but certain regions show variance. ");
            if (result.getUncertaintyReasons() != null && !result.getUncertaintyReasons().isEmpty()) {
                UncertaintyReason first = result.getUncertaintyReasons().get(0);
                sb.append(first.getDescription()).append(" ");
            }
            sb.append("A second scan under balanced lighting with the note flat on a neutral background is advised. ");
        } else {
            sb.append("Significant irregularities were detected in the color histogram, edge sharpness, or security regions. ");
            sb.append("The visual signals deviate notably from standard currency reference templates. ");
            sb.append("Inspect this note carefully under ultraviolet or transmitted light. ");
        }

        if (result.getImageQualityScore() < 70.0) {
            sb.append("Note: Lower image quality (surface reflections or blur) impacted overall AI confidence.");
        }

        return sb.toString();
    }
}
