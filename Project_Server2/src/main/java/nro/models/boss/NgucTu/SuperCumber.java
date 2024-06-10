package nro.models.boss.NgucTu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import nro.models.boss.*;
import nro.models.item.Item;
import nro.models.item.ItemOption;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.server.Client;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.RewardService;
import nro.services.Service;
import nro.utils.Util;

/**
 * @Build by Arriety
 */
public class SuperCumber extends FutureBoss {

    public SuperCumber() {
        super(BossFactory.CUMBER2, BossData.CUMBER2);
    }

    @Override
    protected boolean useSpecialSkill() {
        return false;
    }

    @Override
    public int injured(Player plAtt, int damage, boolean piercing, boolean isMobAttack) {
        if (this.isDie()) {
            return 0;
        } else {
            if (plAtt != null) {
                if (Util.isTrue(25, 70)) {
                    damage = 1;
                    Service.getInstance().chat(this, "Xí hụt..");
                }
            }
            int dame = super.injured(plAtt, damage, piercing, isMobAttack);
            if (this.isDie()) {
                rewards(plAtt);
            }
            return dame;
        }
    }

    @Override
    public void rewards(Player pl) {
        List<Map.Entry<Long, Integer>> entryList = new ArrayList<>(Util.sortHashMapByValue(topDame).entrySet());
        if (!entryList.isEmpty()) {
            Player playerTop = Client.gI().getPlayer(entryList.get(0).getKey());
            if (playerTop != null) {
                Item manhTL = ItemService.gI().createNewItem((short) Util.nextInt(2032, 2036));
                InventoryService.gI().addItemBag(playerTop, manhTL, 0);
                InventoryService.gI().sendItemBags(playerTop);
                Service.getInstance().sendThongBao(playerTop, "Bạn nhận được " + manhTL.template.name);
            }
            if (entryList.size() > 1) {
                Player playerTop2 = Client.gI().getPlayer(entryList.get(1).getKey());
                if (playerTop2 != null) {
                    Item thoivang = ItemService.gI().createNewItem((short) 457);
                    InventoryService.gI().addItemBag(playerTop2, thoivang, 0);
                    InventoryService.gI().sendItemBags(playerTop2);
                    Service.getInstance().sendThongBao(playerTop2, "Bạn nhận được thỏi vàng");
                }
            }
            if (entryList.size() > 2) {
                Player playerTop3 = Client.gI().getPlayer(entryList.get(2).getKey());
                if (playerTop3 != null) {
                    Item mewmew = ItemService.gI().createNewItem((short) Util.nextInt(1150, 1153));
                    InventoryService.gI().addItemBag(playerTop3, mewmew, 0);
                    InventoryService.gI().sendItemBags(playerTop3);
                    Service.getInstance().sendThongBao(playerTop3, "Bạn nhận được " + mewmew.template.name);
                }
            }
        }
        entryList.clear();
        // do than 1/20
        int[] tempIds1 = new int[]{555, 556, 563, 557, 558, 565, 559, 567, 560};
        // Nhan, gang than 1/30
        int[] tempIds2 = new int[]{562, 564, 566, 561};

        int[] tempIds3 = new int[]{16};

        int tempId = -1;
        if (Util.isTrue(1, 20)) {
            tempId = tempIds3[Util.nextInt(0, tempIds3.length - 1)];
        } else if (Util.isTrue(1, 30)) {
            tempId = tempIds1[Util.nextInt(0, tempIds1.length - 1)];
        } else if (Util.isTrue(1, 100)) {
            tempId = tempIds2[Util.nextInt(0, tempIds2.length - 1)];
        }
        if (tempId != -1) {
            ItemMap itemMap = new ItemMap(this.zone, tempId, 1, pl.location.x, this.zone.map.yPhysicInTop(pl.location.x, pl.location.y - 24), pl.id);
            if (tempId == 16) {
                itemMap.options.add(new ItemOption(199, 0));
            } else {
                RewardService.gI().initBaseOptionClothes(itemMap.itemTemplate.id, itemMap.itemTemplate.type, itemMap.options);
                RewardService.gI().initStarOption(itemMap, new RewardService.RatioStar[]{
                    new RewardService.RatioStar((byte) 1, 1, 2),
                    new RewardService.RatioStar((byte) 2, 1, 3),
                    new RewardService.RatioStar((byte) 3, 1, 4),
                    new RewardService.RatioStar((byte) 4, 1, 5),
                    new RewardService.RatioStar((byte) 5, 1, 6),});
            }
            Service.getInstance().dropItemMap(this.zone, itemMap);
        }
        generalRewards(pl);
    }

    @Override
    public void idle() {

    }

    @Override
    public void checkPlayerDie(Player pl) {

    }

    @Override
    public void initTalk() {
        textTalkAfter = new String[]{"Ta đã giấu hết ngọc rồng rồi, các ngươi tìm vô ích hahaha"};
    }

    @Override
    public void leaveMap() {
        this.topDame.clear();
        Service.getInstance().sendTextTime(this, (byte) 10, "", (short) 0);
        Service.getInstance().sendTextTime(this, (byte) 11, "", (short) 0);
        Service.getInstance().sendTextTime(this, (byte) 12, "", (short) 0);
        BossManager.gI().getBossById(BossFactory.CUMBER).setJustRest();
        super.leaveMap();
        BossManager.gI().removeBoss(this);
    }
}
