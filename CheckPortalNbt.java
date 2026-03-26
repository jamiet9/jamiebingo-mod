import java.io.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;

public class CheckPortalNbt {
  private static CompoundTag read(Path p) throws Exception {
    try (InputStream in = Files.newInputStream(p)) {
      try {
        return NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
      } catch (Throwable t) {
      }
    }
    try (InputStream in2 = Files.newInputStream(p)) {
      return NbtIo.read(in2, NbtAccounter.unlimitedHeap());
    }
  }

  private static boolean hasCrying(CompoundTag root) {
    if (root == null) return false;
    Tag palTag = root.get("palette");
    if (palTag instanceof ListTag palette) {
      for (Tag t : palette) {
        if (t instanceof CompoundTag c) {
          String name = c.getString("Name");
          if ("minecraft:crying_obsidian".equals(name)) return true;
        }
      }
    }
    Tag palsTag = root.get("palettes");
    if (palsTag instanceof ListTag palettes) {
      for (Tag entry : palettes) {
        if (entry instanceof ListTag palette) {
          for (Tag t : palette) {
            if (t instanceof CompoundTag c) {
              String name = c.getString("Name");
              if ("minecraft:crying_obsidian".equals(name)) return true;
            }
          }
        }
      }
    }
    return false;
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println("usage: CheckPortalNbt <dir>");
      return;
    }
    Path dir = Paths.get(args[0]);
    try (var s = Files.list(dir)) {
      s.filter(p -> p.toString().endsWith(".nbt")).sorted().forEach(p -> {
        try {
          CompoundTag root = read(p);
          boolean crying = hasCrying(root);
          System.out.println(p.getFileName()+" crying="+crying);
        } catch (Exception e) {
          System.out.println(p.getFileName()+" ERROR "+e.getClass().getSimpleName()+": "+e.getMessage());
        }
      });
    }
  }
}