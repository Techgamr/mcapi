package com.techgamr.mcapi.ctm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class BlockStatus {
    @JsonProperty("blocks")
    private List<Block> blocks;

    public BlockStatus() {}

    public BlockStatus(List<Block> blocks) {
        this.blocks = blocks;
    }

    public List<Block> getBlocks() { return blocks; }
    public void setBlocks(List<Block> blocks) { this.blocks = blocks; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockStatus that = (BlockStatus) o;
        return Objects.equals(blocks, that.blocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blocks);
    }

    @Override
    public String toString() {
        return "BlockStatus{" +
                "blocks=" + blocks +
                '}';
    }
}
