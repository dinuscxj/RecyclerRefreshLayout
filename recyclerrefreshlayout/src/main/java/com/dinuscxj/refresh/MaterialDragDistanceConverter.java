package com.dinuscxj.refresh;

public class MaterialDragDistanceConverter implements IDragDistanceConverter {
    @Override
    public float convert(float scrollDistance, float refreshDistance) {
        float originalDragPercent = scrollDistance / refreshDistance;
        float dragPercent = Math.min(1.0f, Math.abs(originalDragPercent));
        float slingshotDist = refreshDistance;
        float extraOS = Math.abs(scrollDistance) - slingshotDist;
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, slingshotDist * 2.0f) / slingshotDist);
        float tensionPercent = (float) ((tensionSlingshotPercent / 4) -
                Math.pow((tensionSlingshotPercent / 4), 2)) * 2f;
        float extraMove = (slingshotDist) * tensionPercent * 2;

        int convertY = (int) ((slingshotDist * dragPercent) + extraMove);

        return convertY;
    }
}
