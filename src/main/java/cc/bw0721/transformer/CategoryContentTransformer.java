package cc.bw0721.transformer;

import cc.bw0721.asm.ASMTransformer;
import com.andrei1058.bedwars.api.arena.shop.IContentTier;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.shop.main.CategoryContent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class CategoryContentTransformer extends ASMTransformer {
    public CategoryContentTransformer() {
        super(CategoryContent.class);
    }

    @Inject(method = "execute", desc = "(Lorg/bukkit/entity/Player;Lcom/andrei1058/bedwars/shop/ShopCache;I)V")
    public void hookExecute(MethodNode execute) {
        for (AbstractInsnNode insn : execute.instructions) {
            if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                MethodInsnNode method = (MethodInsnNode) insn;
                if (method.owner.equals("com/andrei1058/bedwars/shop/main/CategoryContent")) {
                    if (method.name.equals("calculateMoney")) {
                        method.owner = "cc/bw0721/transformer/CategoryContentTransformer";
                        method.name = "hookCalculateMoney";
                    } else if (method.name.equals("takeMoney")) {
                        method.owner = "cc/bw0721/transformer/CategoryContentTransformer";
                        method.name = "hookTakeMoney";
                    }
                }
            }
        }

        AbstractInsnNode sendMessageInsn = null;
        for (AbstractInsnNode insn : execute.instructions) {
            if (insn.getOpcode() == Opcodes.INVOKEINTERFACE) {
                MethodInsnNode m = (MethodInsnNode) insn;
                if (m.name.equals("sendMessage") && m.owner.equals("org/bukkit/entity/Player")) {
                    AbstractInsnNode cur = insn.getPrevious();
                    boolean found = false;
                    int depth = 0;
                    while (cur != null && depth < 50) {
                        if (cur.getOpcode() == Opcodes.GETSTATIC) {
                            FieldInsnNode field = (FieldInsnNode) cur;
                            if (field.name.equals("SHOP_INSUFFICIENT_MONEY")) {
                                found = true;
                                break;
                            }
                        }
                        cur = cur.getPrevious();
                        depth++;
                    }
                    if (found) {
                        sendMessageInsn = insn;
                        break;
                    }
                }
            }
        }

        if (sendMessageInsn != null) {
            AbstractInsnNode start = null;
            AbstractInsnNode cur = sendMessageInsn.getPrevious();
            while (cur != null) {
                if (cur.getOpcode() == Opcodes.GETSTATIC) {
                    FieldInsnNode field = (FieldInsnNode) cur;
                    if (field.name.equals("SHOP_INSUFFICIENT_MONEY")) {
                        AbstractInsnNode prev = cur.getPrevious();
                        while (prev != null) {
                            if (prev.getOpcode() == Opcodes.ALOAD && ((VarInsnNode)prev).var == 1) {
                                start = prev;
                                break;
                            }
                            prev = prev.getPrevious();
                        }
                        break;
                    }
                }
                cur = cur.getPrevious();
            }

            if (start != null) {
                InsnList replacement = new InsnList();
                replacement.add(new VarInsnNode(Opcodes.ALOAD, 1));
                replacement.add(new VarInsnNode(Opcodes.ALOAD, 4));
                replacement.add(new VarInsnNode(Opcodes.ILOAD, 5));
                replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "cc/bw0721/transformer/CategoryContentTransformer",
                    "hookCantBuy",
                    "(Lorg/bukkit/entity/Player;Lcom/andrei1058/bedwars/api/arena/shop/IContentTier;I)V",
                    false));

                execute.instructions.insertBefore(start, replacement);

                AbstractInsnNode delCur = start;
                while (delCur != null) {
                    AbstractInsnNode next = delCur.getNext();
                    execute.instructions.remove(delCur);
                    if (delCur == sendMessageInsn) break;
                    delCur = next;
                }
            }
        }
    }

    public static int hookCalculateMoney(Player player, Material currency) {
        return CategoryContent.calculateMoney(player, currency);
    }

    public static void hookCantBuy(Player player, IContentTier ct, int money) {
        player.sendMessage(Language.getMsg(player, Messages.SHOP_INSUFFICIENT_MONEY)
            .replace("{currency}", Language.getMsg(player, CategoryContent.getCurrencyMsgPath(ct)))
            .replace("{amount}", String.valueOf(ct.getPrice() - money)));
    }

    public static void hookTakeMoney(Player player, Material currency, int amount) {
        CategoryContent.takeMoney(player, currency, amount);
    }
}
