package com.remitmind.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class CosineSimilarityTest {

    @Test
    void identicalVectorsHaveSimilarityOfOne() {
        float[] vector = {1f, 2f, 3f};

        double similarity = ComplianceDocumentIngestionService.cosineSimilarity(vector, vector);

        assertThat(similarity).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void orthogonalVectorsHaveSimilarityOfZero() {
        float[] a = {1f, 0f};
        float[] b = {0f, 1f};

        double similarity = ComplianceDocumentIngestionService.cosineSimilarity(a, b);

        assertThat(similarity).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void oppositeVectorsHaveSimilarityOfNegativeOne() {
        float[] a = {1f, 1f};
        float[] b = {-1f, -1f};

        double similarity = ComplianceDocumentIngestionService.cosineSimilarity(a, b);

        assertThat(similarity).isCloseTo(-1.0, within(1e-9));
    }

    @Test
    void similarityIsScaleInvariant() {
        float[] a = {3f, 4f};
        float[] scaledA = {6f, 8f};

        double similarity = ComplianceDocumentIngestionService.cosineSimilarity(a, scaledA);

        assertThat(similarity).isCloseTo(1.0, within(1e-9));
    }
}
