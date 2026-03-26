package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.bingo.BingoCard;
import com.jamie.jamiebingo.bingo.BingoLineType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ClientCompletedLines {

    private ClientCompletedLines() {}

    public static final class Line {
        public final BingoLineType type;
        public final int index;

        public Line(BingoLineType type, int index) {
            this.type = type;
            this.index = index;
        }

        public BingoLineType type() {
            return type;
        }

        public int index() {
            return index;
        }
    }

    public static List<Line> getCompletedLines(
            BingoCard card,
            Set<String> completed
    ) {
        List<Line> out = new ArrayList<>();
        int size = card.getSize();

        for (int y = 0; y < size; y++) {
            boolean ok = true;
            for (int x = 0; x < size; x++) {
                if (!completed.contains(card.getSlot(x, y).getId())) {
                    ok = false; break;
                }
            }
            if (ok) out.add(new Line(BingoLineType.ROW, y));
        }

        for (int x = 0; x < size; x++) {
            boolean ok = true;
            for (int y = 0; y < size; y++) {
                if (!completed.contains(card.getSlot(x, y).getId())) {
                    ok = false; break;
                }
            }
            if (ok) out.add(new Line(BingoLineType.COLUMN, x));
        }

        boolean main = true;
        boolean anti = true;

        for (int i = 0; i < size; i++) {
            if (!completed.contains(card.getSlot(i, i).getId())) main = false;
            if (!completed.contains(card.getSlot(size - 1 - i, i).getId())) anti = false;
        }

        if (main) out.add(new Line(BingoLineType.DIAGONAL_MAIN, 0));
        if (anti) out.add(new Line(BingoLineType.DIAGONAL_ANTI, 0));

        return out;
    }
}
