package net.minestom.server.item;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.item.component.CustomData;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

record ItemStackImpl(Material material, int amount, ItemComponentPatch components) implements ItemStack {

    static ItemStack create(Material material, int amount, ItemComponentPatch components) {
        if (amount <= 0) return AIR;
        return new ItemStackImpl(material, amount, components);
    }

    static ItemStack create(Material material, int amount) {
        return create(material, amount, ItemComponentPatch.builder(material).build());
    }

    @Override
    public <T> @Nullable T get(@NotNull ItemComponent<T> component) {
        return components.get(component);
    }

    @Override
    public boolean has(@NotNull ItemComponent<?> component) {
        return components.has(component);
    }

    @Override
    public @NotNull ItemStack with(@NotNull Consumer<ItemStack.@NotNull Builder> consumer) {
        ItemStack.Builder builder = builder();
        consumer.accept(builder);
        return builder.build();
    }

    @Override
    public @NotNull ItemStack withMaterial(@NotNull Material material) {
        return new ItemStackImpl(material, amount, components);
    }

    @Override
    public @NotNull ItemStack withAmount(int amount) {
        return create(material, amount, components);
    }

    @Override
    public @NotNull <T> ItemStack with(@NotNull ItemComponent<T> component, T value) {
        return new ItemStackImpl(material, amount, components.with(component, value));
    }

    @Override
    public @NotNull ItemStack without(@NotNull ItemComponent<?> component) {
        return new ItemStackImpl(material, amount, components.without(component));
    }

    @Override
    public @NotNull ItemStack consume(int amount) {
        int newAmount = amount() - amount;
        if (newAmount <= 0) return AIR;
        return withAmount(newAmount);
    }

    @Override
    public boolean isSimilar(@NotNull ItemStack itemStack) {
        return material == itemStack.material() && components.equals(((ItemStackImpl) itemStack).components);
    }

    @Override
    public @NotNull CompoundBinaryTag toItemNBT() {
//        CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder()
//                .putString("id", material.name())
//                .putByte("Count", (byte) amount);
//        CompoundBinaryTag nbt = meta.toNBT();
//        if (nbt.size() > 0) builder.put("tag", nbt);
//        return builder.build();
        //todo
    }

    @Contract(value = "-> new", pure = true)
    private @NotNull ItemStack.Builder builder() {
        return new Builder(material, amount, components.builder());
    }

    static final class Builder implements ItemStack.Builder {
        final Material material;
        int amount;
        ItemComponentPatch.Builder components;

        Builder(Material material, int amount, ItemComponentPatch.Builder components) {
            this.material = material;
            this.amount = amount;
            this.components = components;
        }

        Builder(Material material, int amount) {
            this.material = material;
            this.amount = amount;
            this.components = ItemComponentPatch.builder(material);
        }

        @Override
        public ItemStack.@NotNull Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        @Override
        public <T> ItemStack.@NotNull Builder set(@NotNull ItemComponent<T> component, T value) {
            components.set(component, value);
            return this;
        }

        @Override
        public ItemStack.@NotNull Builder remove(@NotNull ItemComponent<?> component) {
            components.remove(component);
            return this;
        }

        @Override
        public <T> void setTag(@NotNull Tag<T> tag, @Nullable T value) {
            components.set(ItemComponent.CUSTOM_DATA, components.get(ItemComponent.CUSTOM_DATA, CustomData.EMPTY).withTag(tag, value));
        }

        @Override
        public @NotNull ItemStack build() {
            return ItemStackImpl.create(material, amount, components.build());
        }

    }
}
