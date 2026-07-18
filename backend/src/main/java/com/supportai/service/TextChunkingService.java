package com.supportai.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkingService {

    private static final int CHUNK_SIZE = 800;
    private static final int OVERLAP = 100;

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int length = text.length();

        while (start < length) {
            int end = Math.min(start + CHUNK_SIZE, length);

            if (end < length) {
                int breakAt = findBreakPoint(text, start, end);
                if (breakAt > start) {
                    end = breakAt;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (end >= length) {
                break;
            }

            start = Math.max(end - OVERLAP, start + 1);
        }

        return chunks;
    }

    private int findBreakPoint(String text, int start, int end) {
        for (int i = end; i > start; i--) {
            char c = text.charAt(i - 1);
            if (c == '\n' || c == '.' || c == ' ') {
                return i;
            }
        }
        return end;
    }
}
