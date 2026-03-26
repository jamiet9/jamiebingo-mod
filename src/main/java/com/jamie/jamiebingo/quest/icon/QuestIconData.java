package com.jamie.jamiebingo.quest.icon;

import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

public class QuestIconData {

    public final ItemStack mainIcon;
    public final List<ItemStack> rotatingIcons;
    public final ItemStack cornerIcon;
    public final String numberText;
    public final Identifier mainTexture;
    public final List<Identifier> rotatingTextures;
    public final Identifier cornerTexture;
    public final TextureRegion mainRegion;
    public final TextureRegion cornerRegion;
    public final EntityType<? extends LivingEntity> mainEntityType;
    public final List<EntityType<? extends LivingEntity>> rotatingEntities;
    public final EntityType<? extends LivingEntity> cornerEntityType;
    public final Identifier mainEntityVariant;
    public final List<Identifier> rotatingEntityVariants;
    public final Integer mainEntityColor;
    public final List<Integer> rotatingEntityColors;
    public final Integer cornerEntityColor;
    public final List<Integer> rotatingCornerEntityColors;
    public final List<EntityType<? extends LivingEntity>> rotatingCornerEntities;
    public final List<ItemStack> rotatingCornerIcons;
    public final List<Identifier> rotatingCornerTextures;
    public final int cornerCopies;

    public static class TextureRegion {
        public final Identifier texture;
        public final int u;
        public final int v;
        public final int width;
        public final int height;
        public final int textureWidth;
        public final int textureHeight;

        public TextureRegion(Identifier texture, int u, int v, int width, int height, int textureWidth, int textureHeight) {
            this.texture = texture;
            this.u = u;
            this.v = v;
            this.width = width;
            this.height = height;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
        }
    }

    public QuestIconData(ItemStack mainIcon,
                         List<ItemStack> rotatingIcons,
                         ItemStack cornerIcon,
                         String numberText,
                         Identifier mainTexture,
                         List<Identifier> rotatingTextures,
                         Identifier cornerTexture,
                         TextureRegion mainRegion,
                         TextureRegion cornerRegion,
                         EntityType<? extends LivingEntity> mainEntityType,
                         List<EntityType<? extends LivingEntity>> rotatingEntities,
                         EntityType<? extends LivingEntity> cornerEntityType,
                         List<EntityType<? extends LivingEntity>> rotatingCornerEntities,
                         List<ItemStack> rotatingCornerIcons,
                         List<Identifier> rotatingCornerTextures) {
        this(
                mainIcon,
                rotatingIcons,
                cornerIcon,
                numberText,
                mainTexture,
                rotatingTextures,
                cornerTexture,
                mainRegion,
                cornerRegion,
                mainEntityType,
                rotatingEntities,
                cornerEntityType,
                null,
                List.of(),
                null,
                List.of(),
                null,
                List.of(),
                rotatingCornerEntities,
                rotatingCornerIcons,
                rotatingCornerTextures,
                1
        );
    }

    public QuestIconData(ItemStack mainIcon,
                         List<ItemStack> rotatingIcons,
                         ItemStack cornerIcon,
                         String numberText,
                         Identifier mainTexture,
                         List<Identifier> rotatingTextures,
                         Identifier cornerTexture,
                         TextureRegion mainRegion,
                         TextureRegion cornerRegion,
                         EntityType<? extends LivingEntity> mainEntityType,
                         List<EntityType<? extends LivingEntity>> rotatingEntities,
                         EntityType<? extends LivingEntity> cornerEntityType,
                         Identifier mainEntityVariant,
                         List<Identifier> rotatingEntityVariants,
                         Integer mainEntityColor,
                         List<Integer> rotatingEntityColors,
                         Integer cornerEntityColor,
                         List<Integer> rotatingCornerEntityColors,
                         List<EntityType<? extends LivingEntity>> rotatingCornerEntities,
                         List<ItemStack> rotatingCornerIcons,
                         List<Identifier> rotatingCornerTextures,
                         int cornerCopies) {
        this.mainIcon = mainIcon;
        this.rotatingIcons = rotatingIcons;
        this.cornerIcon = cornerIcon;
        this.numberText = numberText;
        this.mainTexture = mainTexture;
        this.rotatingTextures = rotatingTextures;
        this.cornerTexture = cornerTexture;
        this.mainRegion = mainRegion;
        this.cornerRegion = cornerRegion;
        this.mainEntityType = mainEntityType;
        this.rotatingEntities = rotatingEntities;
        this.cornerEntityType = cornerEntityType;
        this.mainEntityVariant = mainEntityVariant;
        this.rotatingEntityVariants = rotatingEntityVariants;
        this.mainEntityColor = mainEntityColor;
        this.rotatingEntityColors = rotatingEntityColors;
        this.cornerEntityColor = cornerEntityColor;
        this.rotatingCornerEntityColors = rotatingCornerEntityColors;
        this.rotatingCornerEntities = rotatingCornerEntities;
        this.rotatingCornerIcons = rotatingCornerIcons;
        this.rotatingCornerTextures = rotatingCornerTextures;
        this.cornerCopies = cornerCopies;
    }

    public static QuestIconData empty(ItemStack icon) {
        return new QuestIconData(
                icon,
                List.of(),
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                "",
                null,
                List.of(),
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                List.of(),
                null,
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                1
        );
    }

    public static QuestIconData fromItems(
            ItemStack main,
            List<ItemStack> rotating,
            ItemStack corner,
            String numberText
    ) {
        return new QuestIconData(
                main,
                rotating,
                corner,
                numberText,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                List.of(),
                null,
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                1
        );
    }

    public static QuestIconData fromTextures(
            Identifier main,
            List<Identifier> rotating,
            Identifier corner,
            String numberText
    ) {
        return new QuestIconData(
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                List.of(),
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                numberText,
                main,
                rotating,
                corner,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                List.of(),
                null,
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                1
        );
    }

    public static QuestIconData fromRegions(
            TextureRegion mainRegion,
            TextureRegion cornerRegion,
            String numberText
    ) {
        return new QuestIconData(
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                List.of(),
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                numberText,
                null,
                List.of(),
                null,
                mainRegion,
                cornerRegion,
                null,
                List.of(),
                null,
                null,
                List.of(),
                null,
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                1
        );
    }

    public static QuestIconData fromEntities(
            EntityType<? extends LivingEntity> mainEntity,
            List<EntityType<? extends LivingEntity>> rotating,
            EntityType<? extends LivingEntity> cornerEntity,
            String numberText
    ) {
        return new QuestIconData(
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                List.of(),
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                numberText,
                null,
                List.of(),
                null,
                null,
                null,
                mainEntity,
                rotating,
                cornerEntity,
                null,
                List.of(),
                null,
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                1
        );
    }

    public static QuestIconData full(
            ItemStack mainIcon,
            List<ItemStack> rotatingIcons,
            ItemStack cornerIcon,
            String numberText,
            Identifier mainTexture,
            List<Identifier> rotatingTextures,
            Identifier cornerTexture,
            TextureRegion mainRegion,
            TextureRegion cornerRegion,
            EntityType<? extends LivingEntity> mainEntityType,
            List<EntityType<? extends LivingEntity>> rotatingEntities,
            EntityType<? extends LivingEntity> cornerEntityType,
            List<EntityType<? extends LivingEntity>> rotatingCornerEntities,
            List<ItemStack> rotatingCornerIcons,
            List<Identifier> rotatingCornerTextures
    ) {
        return new QuestIconData(
                mainIcon,
                rotatingIcons,
                cornerIcon,
                numberText,
                mainTexture,
                rotatingTextures,
                cornerTexture,
                mainRegion,
                cornerRegion,
                mainEntityType,
                rotatingEntities,
                cornerEntityType,
                null,
                List.of(),
                null,
                List.of(),
                null,
                List.of(),
                rotatingCornerEntities,
                rotatingCornerIcons,
                rotatingCornerTextures,
                1
        );
    }

    public QuestIconData withCornerCopies(int copies) {
        return new QuestIconData(
                mainIcon,
                rotatingIcons,
                cornerIcon,
                numberText,
                mainTexture,
                rotatingTextures,
                cornerTexture,
                mainRegion,
                cornerRegion,
                mainEntityType,
                rotatingEntities,
                cornerEntityType,
                mainEntityVariant,
                rotatingEntityVariants,
                mainEntityColor,
                rotatingEntityColors,
                cornerEntityColor,
                rotatingCornerEntityColors,
                rotatingCornerEntities,
                rotatingCornerIcons,
                rotatingCornerTextures,
                copies
        );
    }

    public QuestIconData withMainEntityVariant(Identifier variant) {
        return new QuestIconData(
                mainIcon,
                rotatingIcons,
                cornerIcon,
                numberText,
                mainTexture,
                rotatingTextures,
                cornerTexture,
                mainRegion,
                cornerRegion,
                mainEntityType,
                rotatingEntities,
                cornerEntityType,
                variant,
                rotatingEntityVariants,
                mainEntityColor,
                rotatingEntityColors,
                cornerEntityColor,
                rotatingCornerEntityColors,
                rotatingCornerEntities,
                rotatingCornerIcons,
                rotatingCornerTextures,
                cornerCopies
        );
    }

    public QuestIconData withRotatingEntityVariants(List<Identifier> variants) {
        return new QuestIconData(
                mainIcon,
                rotatingIcons,
                cornerIcon,
                numberText,
                mainTexture,
                rotatingTextures,
                cornerTexture,
                mainRegion,
                cornerRegion,
                mainEntityType,
                rotatingEntities,
                cornerEntityType,
                mainEntityVariant,
                variants,
                mainEntityColor,
                rotatingEntityColors,
                cornerEntityColor,
                rotatingCornerEntityColors,
                rotatingCornerEntities,
                rotatingCornerIcons,
                rotatingCornerTextures,
                cornerCopies
        );
    }
}
