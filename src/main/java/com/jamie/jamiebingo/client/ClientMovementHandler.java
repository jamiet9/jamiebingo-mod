package com.jamie.jamiebingo.client;


import com.jamie.jamiebingo.client.ClientMinecraftUtil;
import com.jamie.jamiebingo.addons.effects.EffectRandomMovement;
import com.jamie.jamiebingo.util.ClientInputUtil;
import com.jamie.jamiebingo.util.Vec2Util;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.MovementInputUpdateEvent;

public class ClientMovementHandler {

    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (ClientMinecraftUtil.getPlayer() == null) return;

        var player = ClientMinecraftUtil.getPlayer();
        Object input = ClientInputUtil.getInput(player);
        if (input == null) return;
        var move = ClientInputUtil.getMoveVector(input);
        if (move == null) return;
        float[] remapped = EffectRandomMovement.remapMovement(
                Vec2Util.getY(move),
                Vec2Util.getX(move)
        );
        ClientInputUtil.setMoveVector(input, new net.minecraft.world.phys.Vec2(remapped[1], remapped[0]));
    }
}

