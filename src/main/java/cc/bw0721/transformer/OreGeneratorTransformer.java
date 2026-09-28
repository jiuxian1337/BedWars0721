package cc.bw0721.transformer;

import cc.bw0721.asm.ASMTransformer;
import cc.bw0721.utils.ReflectionUtils;
import cc.bw0721.utils.XPUtils;
import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.OreGenerator;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class OreGeneratorTransformer extends ASMTransformer {

    public OreGeneratorTransformer() {
        super(OreGenerator.class);
    }

    @Inject(
        method = "spawn",
        desc = "()V"
    )
    public void hookSpawn(MethodNode spawn) {
        for (int i = 0; i < spawn.instructions.size(); i++) {
            AbstractInsnNode insn = spawn.instructions.get(i);
            if (insn.getOpcode() != Opcodes.ALOAD) continue;

            VarInsnNode var = (VarInsnNode) insn;
            if (var.var != 1) continue;

            AbstractInsnNode arrLen = insn.getNext();
            if (arrLen == null || arrLen.getOpcode() != Opcodes.ARRAYLENGTH) continue;

            AbstractInsnNode iconst = arrLen.getNext();
            if (iconst == null || iconst.getOpcode() != Opcodes.ICONST_1) continue;

            AbstractInsnNode branch = iconst.getNext();
            if (!(branch instanceof JumpInsnNode)) continue;
            if (branch.getOpcode() != Opcodes.IF_ICMPGT) continue;

            JumpInsnNode jump = (JumpInsnNode) branch;
            LabelNode newLabel = new LabelNode();
            InsnList call = new InsnList();
            call.add(newLabel);
            call.add(new VarInsnNode(Opcodes.ALOAD, 0));
            call.add(new VarInsnNode(Opcodes.ALOAD, 1));
            call.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                    Type.getInternalName(OreGeneratorTransformer.class),
                "handleTeamSplit",
                "(Lcom/andrei1058/bedwars/arena/OreGenerator;[Ljava/lang/Object;)V",
                false
            ));
            call.add(new InsnNode(Opcodes.RETURN));

            jump.label = newLabel;
            spawn.instructions.add(call);
            return;
        }
    }

    @SuppressWarnings("deprecation")
    public static void handleTeamSplit(OreGenerator generator, Object[] players) {
        try {
            if (generator == null || players == null || players.length == 0) return;
            IArena arena = Arena.getArenaByPlayer((Player)players[0]);
            if (arena == null) return;
            boolean xpArena = XPUtils.isXPArena(arena.getArenaName());
            for(Object o : players) {
                Player player = (Player)o;
                ItemStack item = ((ItemStack) ReflectionUtils.getFieldValue(OreGenerator.class, "ore", generator)).clone();
                item.setAmount((Integer) ReflectionUtils.getFieldValue(OreGenerator.class, "amount", generator));

                int xp = XPUtils.getExp(item.getType()) * item.getAmount();
                if (xpArena && xp != 0) {
                        player.playSound(player.getLocation(), XPUtils.getSound(), 0.6f, 1.3f);
                        player.setLevel(player.getLevel() + xp);
                } else {

                    player.playSound(player.getLocation(), Sound.valueOf(BedWars.getForCurrentVersion("ITEM_PICKUP", "ENTITY_ITEM_PICKUP", "ENTITY_ITEM_PICKUP")), 0.6F, 1.3F);

                    for (ItemStack value : player.getInventory().addItem(new ItemStack[]{item}).values()) {
                        ReflectionUtils.getMethod(OreGenerator.class, "dropItem", Location.class, int.class)
                                .invoke(generator, player.getLocation(), value.getAmount());
                    }
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
