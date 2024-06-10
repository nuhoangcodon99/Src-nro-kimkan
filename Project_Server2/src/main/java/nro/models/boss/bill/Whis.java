package nro.models.boss.bill;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossFactory;
import nro.models.boss.BossManager;
import nro.models.player.Player;
import nro.utils.Util;

/**
 *
 * @author Arrriety
 *
 */
public class Whis extends Boss {

    public Whis() {
        super(BossFactory.WHIS, BossData.WHIS);
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
            if (!piercing && Util.isTrue(90, 100)) {
                this.chat("Xí hụt lêu lêu");
                return 0;
            }
            if (this.isDie()) {
                rewards(plAtt);
            }
            return super.injured(plAtt, 1, piercing, isMobAttack);
        }
    }

    @Override
    public void rewards(Player pl) {
        if (pl != null) {
            this.dropItemReward(2040, (int) pl.id);
            generalRewards(pl);
        }
    }

    @Override
    public void idle() {
    }

    @Override
    public void leaveMap() {
        super.leaveMap();
        BossManager.gI().removeBoss(this);
        BossManager.gI().getBossById(BossFactory.BILL).changeToAttack();
    }

    @Override
    public void checkPlayerDie(Player pl) {
    }

    @Override
    public void initTalk() {
    }

    @Override
    public void joinMap() {
        super.joinMap();
        if (this.zone != null) {
            BossFactory.createBoss(BossFactory.BILL).zone = this.zone;
        } else {
            BossManager.gI().removeBoss(this);
        }
    }
}
