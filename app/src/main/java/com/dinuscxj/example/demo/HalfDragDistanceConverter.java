package com.dinuscxj.example.demo;

import com.dinuscxj.refresh.IDragDistanceConverter;

public class HalfDragDistanceConverter implements IDragDistanceConverter {

    @Override
    public float convert(float scrollDistance, float refreshDistance) {
        return scrollDistance * 0.5f;
    }
}
