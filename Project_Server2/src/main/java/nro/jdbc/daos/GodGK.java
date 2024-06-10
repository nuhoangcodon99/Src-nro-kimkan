package nro.jdbc.daos;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import nro.card.Card;
import nro.card.CollectionBook;
import nro.consts.ConstMap;
import nro.consts.ConstPlayer;
import nro.jdbc.DBService;
import nro.manager.AchiveManager;
import nro.manager.PetFollowManager;
import nro.models.player.PetFollow;
import nro.models.clan.Clan;
import nro.models.clan.ClanMember;
import nro.models.item.Item;
import nro.models.item.ItemOption;
import nro.models.item.ItemTime;
import nro.models.npc.specialnpc.MabuEgg;
import nro.models.npc.specialnpc.MagicTree;
import nro.models.player.*;
import nro.models.skill.Skill;
import nro.models.task.Achivement;
import nro.models.task.AchivementTemplate;
import nro.models.task.TaskMain;
import nro.server.Client;
import nro.server.Manager;
import nro.server.io.Session;
import nro.server.model.AntiLogin;
import nro.services.*;
import nro.utils.SkillUtil;
import nro.utils.TimeUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import nro.models.npc.specialnpc.BillEgg;
import nro.models.npc.specialnpc.EggLinhThu;

/**
 * @Build Arriety
 */
public class GodGK {

    public static boolean login(Session session, AntiLogin al) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = DBService.gI().getConnectionForLogin();
            String query = "select * from account where username = ? and password = ? limit 1";
            ps = conn.prepareStatement(query);
            ps.setString(1, session.uu);
            ps.setString(2, session.pp);
            rs = ps.executeQuery();
            if (rs.next()) {
                session.userId = rs.getInt("account.id");
                Session plInGame = Client.gI().getSession(session);
                if (plInGame != null) {
                    Service.getInstance().sendThongBaoOK(plInGame, "Máy chủ tắt hoặc mất sóng!");
                    Client.gI().kickSession(plInGame);
                    Service.getInstance().sendThongBaoOK(session, "Máy chủ tắt hoặc mất sóng!");
                    return false;
                }

                session.isAdmin = rs.getBoolean("is_admin");
                session.lastTimeLogout = rs.getTimestamp("last_time_logout").getTime();
                session.actived = rs.getBoolean("active");
                session.goldBar = rs.getInt("account.thoi_vang");
                session.vnd = rs.getInt("account.vnd");
                session.poinCharging = rs.getInt("account.pointNap");
                session.dataReward = rs.getString("reward");
                if (rs.getTimestamp("last_time_login").getTime() > session.lastTimeLogout) {
                    Service.getInstance().sendThongBaoOK(session, "Tài khoản đang đăng nhập máy chủ khác!");
                    return false;
                }
                if (session.version < 225) {
                    Service.getInstance().sendThongBaoOK(session, "Hãy tải phiên bản từ Trang Chủ");
                } else if (rs.getBoolean("ban")) {
                    Service.getInstance().sendThongBaoOK(session, "Tài khoản đã bị khóa do vi phạm điều khoản!");
                } else {
                    long lastTimeLogout = rs.getTimestamp("last_time_logout").getTime();
                    int secondsPass = (int) ((System.currentTimeMillis() - lastTimeLogout) / 1000);
                    if (secondsPass < Manager.SECOND_WAIT_LOGIN && !session.isAdmin) {
                        Service.getInstance().sendThongBaoOK(session, "Vui lòng chờ " + (Manager.SECOND_WAIT_LOGIN - secondsPass) + " giây để đăng nhập lại.");
                    }
                }
                al.reset();
                return true;
            } else {
                Service.getInstance().sendThongBaoOK(session, "Thông tin tài khoản hoặc mật khẩu không chính xác");
                al.wrong();
                // Anti login
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                }
            }
        }
        return false;
    }

    public static Player loadPlayer(Session session) {
        try {
            Connection connection = DBService.gI().getConnectionForLogin();
            PreparedStatement ps = connection.prepareStatement("select * from player where account_id = ? limit 1");
            ps.setInt(1, session.userId);
            ResultSet rs = ps.executeQuery();
            try {
                if (rs.next()) {
                    int plHp = 200000000;
                    int plMp = 200000000;
                    JSONValue jv = new JSONValue();
                    JSONArray dataArray = null;
                    JSONObject dataObject = null;
                    Player player = new Player();
                    //base info
                    player.id = rs.getInt("id");
                    player.name = rs.getString("name");
                    player.head = rs.getShort("head");
                    player.gender = rs.getByte("gender");
                    player.haveTennisSpaceShip = rs.getBoolean("have_tennis_space_ship");
                    int clanId = rs.getInt("clan_id_sv" + Manager.SERVER);
                    if (clanId != -1) {
                        Clan clan = ClanService.gI().getClanById(clanId);
                        if (clan != null) {
                            for (ClanMember cm : clan.getMembers()) {
                                if (cm.id == player.id) {
                                    clan.addMemberOnline(player);
                                    player.clan = clan;
                                    player.clanMember = cm;
                                    player.setBuff(clan.getBuff());
                                    break;
                                }
                            }
                        }
                    }
                    // diem su kien
                    int evPoint = rs.getInt("event_point");
                    player.event.setEventPoint(evPoint);

                    dataArray = (JSONArray) JSONValue.parse(rs.getString("sk_tet"));
                    int timeBanhTet = Integer.parseInt(dataArray.get(0).toString());
                    int timeBanhChung = Integer.parseInt(dataArray.get(1).toString());
                    boolean isNauBanhTet = Integer.parseInt(dataArray.get(2).toString()) == 1;
                    boolean isNauBanhChung = Integer.parseInt(dataArray.get(3).toString()) == 1;
                    boolean receivedLuckMoney = Integer.parseInt(dataArray.get(4).toString()) == 1;

                    player.event.setTimeCookTetCake(timeBanhTet);
                    player.event.setTimeCookChungCake(timeBanhChung);
                    player.event.setCookingTetCake(isNauBanhTet);
                    player.event.setCookingChungCake(isNauBanhChung);
                    player.event.setReceivedLuckyMoney(receivedLuckMoney);
                    dataArray.clear();

                    //data kim lượng
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("data_inventory"));
                    player.inventory.gold = Long.parseLong(dataArray.get(0).toString());
                    player.inventory.gem = Integer.parseInt(dataArray.get(1).toString());
                    player.inventory.ruby = Integer.parseInt(dataArray.get(2).toString());
                    if (dataArray.size() >= 4) {
                        player.inventory.goldLimit = Long.parseLong(dataArray.get(3).toString());
                    }
                    dataArray.clear();
                    player.event.setDiemTichLuy(session.diemTichNap);
                    player.event.setMocNapDaNhan(rs.getInt("moc_nap"));
                    player.server = session.server;
                    //data tọa độ
                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("data_location"));
                        player.location.x = Integer.parseInt(dataArray.get(0).toString());
                        player.location.y = Integer.parseInt(dataArray.get(1).toString());
                        int mapId = Integer.parseInt(dataArray.get(2).toString());
                        if (MapService.gI().isMapDoanhTrai(mapId) || MapService.gI().isMapBlackBallWar(mapId)
                                || MapService.gI().isMapBanDoKhoBau(mapId) || mapId == 126 || mapId == ConstMap.CON_DUONG_RAN_DOC
                                || mapId == ConstMap.CON_DUONG_RAN_DOC_142 || mapId == ConstMap.CON_DUONG_RAN_DOC_143 || mapId == ConstMap.HOANG_MAC) {
                            mapId = player.gender + 21;
                            player.location.x = 300;
                            player.location.y = 336;
                        }
                        player.zone = MapService.gI().getMapCanJoin(player, mapId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    dataArray.clear();

                    //data chỉ số
                    dataArray = (JSONArray) jv.parse(rs.getString("data_point"));
                    plMp = Integer.parseInt(dataArray.get(1).toString());
                    player.nPoint.mpg = Integer.parseInt(dataArray.get(2).toString());
                    player.nPoint.critg = Byte.parseByte(dataArray.get(3).toString());
                    player.nPoint.limitPower = Byte.parseByte(dataArray.get(4).toString());
                    player.nPoint.stamina = Short.parseShort(dataArray.get(5).toString());
                    plHp = Integer.parseInt(dataArray.get(6).toString());
                    player.nPoint.defg = Integer.parseInt(dataArray.get(7).toString());
                    player.nPoint.tiemNang = Long.parseLong(dataArray.get(8).toString());
                    player.nPoint.maxStamina = Short.parseShort(dataArray.get(9).toString());
                    player.nPoint.dameg = Integer.parseInt(dataArray.get(10).toString());
                    player.nPoint.power = Long.parseLong(dataArray.get(11).toString());
                    player.nPoint.hpg = Integer.parseInt(dataArray.get(12).toString());
                    dataArray.clear();

                    //data đậu thần
                    dataArray = (JSONArray) jv.parse(rs.getString("data_magic_tree"));
                    boolean isUpgrade = Byte.parseByte(dataArray.get(0).toString()) == 1;
                    long lastTimeUpgrade = Long.parseLong(dataArray.get(1).toString());
                    byte level = Byte.parseByte(dataArray.get(2).toString());
                    long lastTimeHarvest = Long.parseLong(dataArray.get(3).toString());
                    byte currPea = Byte.parseByte(dataArray.get(4).toString());
                    player.magicTree = new MagicTree(player, level, currPea, lastTimeHarvest, isUpgrade, lastTimeUpgrade);
                    dataArray.clear();

                    //data phần thưởng sao đen
                    dataArray = (JSONArray) jv.parse(rs.getString("data_black_ball"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        JSONArray reward = (JSONArray) jv.parse(String.valueOf(dataArray.get(i)));
                        player.rewardBlackBall.timeOutOfDateReward[i] = Long.parseLong(reward.get(0).toString());
                        player.rewardBlackBall.lastTimeGetReward[i] = Long.parseLong(reward.get(1).toString());
                        reward.clear();
                    }
                    dataArray.clear();

                    //data body
                    dataArray = (JSONArray) jv.parse(rs.getString("items_body"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        Item item = null;
                        dataObject = (JSONObject) dataArray.get(i);
                        short tempId = Short.parseShort(String.valueOf(dataObject.get("temp_id")));
                        if (tempId != -1) {
                            item = ItemService.gI().createNewItem(tempId, Integer.parseInt(String.valueOf(dataObject.get("quantity"))));
                            JSONArray options = (JSONArray) dataObject.get("option");
                            for (int j = 0; j < options.size(); j++) {
                                JSONArray opt = (JSONArray) options.get(j);
                                item.itemOptions.add(new ItemOption(Integer.parseInt(String.valueOf(opt.get(0))), Integer.parseInt(String.valueOf(opt.get(1)))));
                            }
                            item.createTime = Long.parseLong(String.valueOf(dataObject.get("create_time")));
                            if (ItemService.gI().isOutOfDateTime(item)) {
                                item = ItemService.gI().createItemNull();
                            }
                        } else {
                            item = ItemService.gI().createItemNull();
                        }
                        player.inventory.itemsBody.add(item);
                    }
                    dataArray.clear();
                    dataObject.clear();

                    //data bag
                    dataArray = (JSONArray) jv.parse(rs.getString("items_bag"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        Item item = null;
                        dataObject = (JSONObject) dataArray.get(i);
                        short tempId = Short.parseShort(String.valueOf(dataObject.get("temp_id")));
                        if (tempId != -1) {
                            item = ItemService.gI().createNewItem(tempId, Integer.parseInt(String.valueOf(dataObject.get("quantity"))));
                            JSONArray options = (JSONArray) dataObject.get("option");
                            for (int j = 0; j < options.size(); j++) {
                                JSONArray opt = (JSONArray) options.get(j);
                                item.itemOptions.add(new ItemOption(Integer.parseInt(String.valueOf(opt.get(0))),
                                        Integer.parseInt(String.valueOf(opt.get(1)))));
                            }
                            item.createTime = Long.parseLong(String.valueOf(dataObject.get("create_time")));
                            if (ItemService.gI().isOutOfDateTime(item)) {
                                item = ItemService.gI().createItemNull();
                            }
                        } else {
                            item = ItemService.gI().createItemNull();
                        }
                        player.inventory.itemsBag.add(item);
                    }
                    dataArray.clear();
                    dataObject.clear();

                    //data box
                    dataArray = (JSONArray) jv.parse(rs.getString("items_box"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        Item item = null;
                        dataObject = (JSONObject) dataArray.get(i);
                        short tempId = Short.parseShort(String.valueOf(dataObject.get("temp_id")));
                        if (tempId != -1) {
                            item = ItemService.gI().createNewItem(tempId, Integer.parseInt(String.valueOf(dataObject.get("quantity"))));
                            JSONArray options = (JSONArray) dataObject.get("option");
                            for (int j = 0; j < options.size(); j++) {
                                JSONArray opt = (JSONArray) options.get(j);
                                item.itemOptions.add(new ItemOption(Integer.parseInt(String.valueOf(opt.get(0))),
                                        Integer.parseInt(String.valueOf(opt.get(1)))));
                            }
                            item.createTime = Long.parseLong(String.valueOf(dataObject.get("create_time")));
                            if (ItemService.gI().isOutOfDateTime(item)) {
                                item = ItemService.gI().createItemNull();
                            }
                        } else {
                            item = ItemService.gI().createItemNull();
                        }
                        player.inventory.itemsBox.add(item);
                    }
                    dataArray.clear();
                    dataObject.clear();

                    //data box lucky round
                    dataArray = (JSONArray) jv.parse(rs.getString("items_box_lucky_round"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        Item item = null;
                        dataObject = (JSONObject) dataArray.get(i);
                        short tempId = Short.parseShort(String.valueOf(dataObject.get("temp_id")));
                        if (tempId != -1) {
                            item = ItemService.gI().createNewItem(tempId, Integer.parseInt(String.valueOf(dataObject.get("quantity"))));
                            JSONArray options = (JSONArray) dataObject.get("option");
                            for (int j = 0; j < options.size(); j++) {
                                JSONArray opt = (JSONArray) options.get(j);
                                item.itemOptions.add(new ItemOption(Integer.parseInt(String.valueOf(opt.get(0))),
                                        Integer.parseInt(String.valueOf(opt.get(1)))));
                            }
                        } else {
                            item = ItemService.gI().createItemNull();
                        }
                        player.inventory.itemsBoxCrackBall.add(item);
                    }
                    dataArray.clear();
                    dataObject.clear();

                    //data friends
                    dataArray = (JSONArray) jv.parse(rs.getString("friends"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        dataObject = (JSONObject) dataArray.get(i);
                        Friend friend = new Friend();
                        friend.id = Integer.parseInt(String.valueOf(dataObject.get("id")));
                        friend.name = String.valueOf(dataObject.get("name"));
                        friend.head = Short.parseShort(String.valueOf(dataObject.get("head")));
                        friend.body = Short.parseShort(String.valueOf(dataObject.get("body")));
                        friend.leg = Short.parseShort(String.valueOf(dataObject.get("leg")));
                        friend.bag = Byte.parseByte(String.valueOf(dataObject.get("bag")));
                        friend.power = Long.parseLong(String.valueOf(dataObject.get("power")));
                        player.friends.add(friend);
                        dataObject.clear();
                    }
                    dataArray.clear();

                    //data enemies
                    dataArray = (JSONArray) jv.parse(rs.getString("enemies"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        dataObject = (JSONObject) dataArray.get(i);
                        Enemy enemy = new Enemy();
                        enemy.id = Integer.parseInt(String.valueOf(dataObject.get("id")));
                        enemy.name = String.valueOf(dataObject.get("name"));
                        enemy.head = Short.parseShort(String.valueOf(dataObject.get("head")));
                        enemy.body = Short.parseShort(String.valueOf(dataObject.get("body")));
                        enemy.leg = Short.parseShort(String.valueOf(dataObject.get("leg")));
                        enemy.bag = Byte.parseByte(String.valueOf(dataObject.get("bag")));
                        enemy.power = Long.parseLong(String.valueOf(dataObject.get("power")));
                        player.enemies.add(enemy);
                        dataObject.clear();
                    }
                    dataArray.clear();

                    //data nội tại
                    dataArray = (JSONArray) jv.parse(rs.getString("data_intrinsic"));
                    byte intrinsicId = Byte.parseByte(dataArray.get(0).toString());
                    player.playerIntrinsic.intrinsic = IntrinsicService.gI().getIntrinsicById(intrinsicId);
                    player.playerIntrinsic.intrinsic.param1 = Short.parseShort(dataArray.get(1).toString());
                    player.playerIntrinsic.countOpen = Byte.parseByte(dataArray.get(2).toString());
                    player.playerIntrinsic.intrinsic.param2 = Short.parseShort(dataArray.get(3).toString());
                    dataArray.clear();

                    dataArray = (JSONArray) jv.parse(rs.getString("time_may_do"));
                    int maycoin = Integer.parseInt(String.valueOf(dataArray.get(0)));
                    player.itemTime.timeMayDo = System.currentTimeMillis() - (ItemTime.TIME_MAY_DO - maycoin);
                    player.itemTime.isMayDo = maycoin != 0;
                    dataArray.clear();

                    dataArray = (JSONArray) jv.parse(rs.getString("item_new_time"));
                    int duoiKhi = Integer.parseInt(String.valueOf(dataArray.get(0)));
                    int timeBanh1Trung = Integer.parseInt(String.valueOf(dataArray.get(1)));
                    int timeBanh2Trung = Integer.parseInt(String.valueOf(dataArray.get(2)));
                    int rateDraHit = 0;
                    int rateDame = 0;
                    int rateHPKI = 0;
                    int timeMa = 0;
                    if (dataArray.size() >= 7) {
                        rateDraHit = Integer.parseInt(String.valueOf(dataArray.get(3)));
                        rateDame = Integer.parseInt(String.valueOf(dataArray.get(4)));
                        rateHPKI = Integer.parseInt(String.valueOf(dataArray.get(5)));
                        timeMa = Integer.parseInt(String.valueOf(dataArray.get(6)));
                    }
                    player.itemTime.lastTimeDuoiKhi = System.currentTimeMillis() - (ItemTime.TIME_MAY_DO - duoiKhi);
                    player.itemTime.lastTimeBanhTrungThu1Trung = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBanh1Trung);
                    player.itemTime.lastTimeBanhTrungThu2Trung = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBanh2Trung);
                    player.itemTime.lastTimerateHit = System.currentTimeMillis() - (ItemTime.TIME_ITEM - rateDraHit);
                    player.itemTime.lastTimeDameDr = System.currentTimeMillis() - (ItemTime.TIME_MAY_DO - rateDame);
                    player.itemTime.lastTimerateHPKI = System.currentTimeMillis() - (ItemTime.TIME_MAY_DO - rateHPKI);
                    player.itemTime.lastTimeMaTroi = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeMa);
                    player.itemTime.isDuoiKhi = duoiKhi != 0;
                    player.itemTime.isBanhTrungThu1Trung = timeBanh1Trung != 0;
                    player.itemTime.isBanhTrungThu2Trung = timeBanh2Trung != 0;
                    player.itemTime.rateDragonHit = rateDraHit != 0;
                    player.itemTime.rateDame = rateDame != 0;
                    player.itemTime.rateHPKI = rateHPKI != 0;
                    player.itemTime.isMaTroi = timeMa != 0;
                    dataArray.clear();

                    //data item time
                    dataArray = (JSONArray) jv.parse(rs.getString("data_item_time"));
                    int timeBoKhi = Integer.parseInt(dataArray.get(0).toString());
                    int timeAnDanh = Integer.parseInt(dataArray.get(1).toString());
                    int timeOpenPower = Integer.parseInt(dataArray.get(2).toString());
                    int timeCuongNo = Integer.parseInt(dataArray.get(3).toString());
                    int timeBoHuyet = Integer.parseInt(dataArray.get(5).toString());
                    int timeGiapXen = Integer.parseInt(dataArray.get(8).toString());
                    int timeMayDo = 0;
                    int timeMeal = 0;
                    int iconMeal = 0;
                    try {
                        timeMayDo = Integer.parseInt(dataArray.get(4).toString());
                        timeMeal = Integer.parseInt(dataArray.get(7).toString());
                        iconMeal = Integer.parseInt(dataArray.get(6).toString());
                    } catch (Exception e) {
                    }
                    int timeBanhChung1 = 0;
                    int timeBanhTet1 = 0;
                    int timeBoKhi2 = 0;
                    int timeGiapXen2 = 0;
                    int timeCuongNo2 = 0;
                    int timeBoHuyet2 = 0;
                    if (dataArray.size() >= 15) {
                        timeBanhChung1 = Integer.parseInt(dataArray.get(9).toString());
                        timeBanhTet1 = Integer.parseInt(dataArray.get(10).toString());
                        timeBoKhi2 = Integer.parseInt(dataArray.get(11).toString());
                        timeGiapXen2 = Integer.parseInt(dataArray.get(12).toString());
                        timeCuongNo2 = Integer.parseInt(dataArray.get(13).toString());
                        timeBoHuyet2 = Integer.parseInt(dataArray.get(14).toString());
                    }
                    player.itemTime.lastTimeBoHuyet = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBoHuyet);
                    player.itemTime.lastTimeBoKhi = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBoKhi);
                    player.itemTime.lastTimeGiapXen = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeGiapXen);
                    player.itemTime.lastTimeCuongNo = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeCuongNo);
                    player.itemTime.lastTimeBoHuyet2 = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBoHuyet2);
                    player.itemTime.lastTimeBoKhi2 = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBoKhi2);
                    player.itemTime.lastTimeGiapXen2 = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeGiapXen2);
                    player.itemTime.lastTimeCuongNo2 = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeCuongNo2);
                    player.itemTime.lastTimeAnDanh = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeAnDanh);
                    player.itemTime.lastTimeOpenPower = System.currentTimeMillis() - (ItemTime.TIME_OPEN_POWER - timeOpenPower);
                    player.itemTime.lastTimeUseMayDo = System.currentTimeMillis() - (ItemTime.TIME_MAY_DO - timeMayDo);
                    player.itemTime.lastTimeEatMeal = System.currentTimeMillis() - (ItemTime.TIME_EAT_MEAL - timeMeal);
                    player.itemTime.lastTimeBanhChung = System.currentTimeMillis() - (ItemTime.TIME_EAT_MEAL - timeBanhChung1);
                    player.itemTime.lastTimeBanhTet = System.currentTimeMillis() - (ItemTime.TIME_EAT_MEAL - timeBanhTet1);
                    player.itemTime.iconMeal = iconMeal;
                    player.itemTime.isUseBoHuyet = timeBoHuyet != 0;
                    player.itemTime.isUseBoKhi = timeBoKhi != 0;
                    player.itemTime.isUseGiapXen = timeGiapXen != 0;
                    player.itemTime.isUseCuongNo = timeCuongNo != 0;
                    player.itemTime.isUseBoHuyet2 = timeBoHuyet2 != 0;
                    player.itemTime.isUseBoKhi2 = timeBoKhi2 != 0;
                    player.itemTime.isUseGiapXen2 = timeGiapXen2 != 0;
                    player.itemTime.isUseCuongNo2 = timeCuongNo2 != 0;
                    player.itemTime.isUseAnDanh = timeAnDanh != 0;
                    player.itemTime.isOpenPower = timeOpenPower != 0;
                    player.itemTime.isUseMayDo = timeMayDo != 0;
                    player.itemTime.isEatMeal = timeMeal != 0;
                    player.itemTime.isUseBanhChung = timeBanhChung1 != 0;
                    player.itemTime.isUseBanhTet = timeBanhTet1 != 0;
                    dataArray.clear();

                    //data nhiệm vụ
                    dataArray = (JSONArray) jv.parse(rs.getString("data_task"));
                    TaskMain taskMain = TaskService.gI().getTaskMainById(player, Byte.parseByte(dataArray.get(1).toString()));
                    taskMain.subTasks.get(Integer.parseInt(dataArray.get(2).toString())).count = Short.parseShort(dataArray.get(0).toString());
                    taskMain.index = Byte.parseByte(dataArray.get(2).toString());
                    player.playerTask.taskMain = taskMain;
                    dataArray.clear();

                    //data nhiệm vụ hàng ngày
                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("data_side_task"));
                        String format = "dd-MM-yyyy";
                        long receivedTime = Long.parseLong(String.valueOf(dataArray.get(4)));
                        Date date = new Date(receivedTime);
                        if (TimeUtil.formatTime(date, format).equals(TimeUtil.formatTime(new Date(), format))) {
                            player.playerTask.sideTask.level = Integer.parseInt(String.valueOf(dataArray.get(0).toString()));
                            player.playerTask.sideTask.count = Integer.parseInt(dataArray.get(1).toString());
                            player.playerTask.sideTask.leftTask = Integer.parseInt(String.valueOf(dataArray.get(2).toString()));
                            player.playerTask.sideTask.template = TaskService.gI().getSideTaskTemplateById(Integer.parseInt(dataArray.get(3).toString()));
                            player.playerTask.sideTask.maxCount = Integer.parseInt(String.valueOf(dataArray.get(5).toString()));
                            player.playerTask.sideTask.receivedTime = receivedTime;
                        }
                    } catch (Exception e) {
                    }

                    dataArray = (JSONArray) jv.parse(rs.getString("achivements"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        dataObject = (JSONObject) jv.parse(String.valueOf(dataArray.get(i)));
                        Achivement achivement = new Achivement();
                        achivement.setId(Integer.parseInt(dataObject.get("id").toString()));
                        achivement.setCount(Integer.parseInt(dataObject.get("count").toString()));
                        achivement.setFinish(Integer.parseInt(dataObject.get("finish").toString()) == 1);
                        achivement.setReceive(Integer.parseInt(dataObject.get("receive").toString()) == 1);
                        AchivementTemplate a = AchiveManager.getInstance().findByID(achivement.getId());
                        achivement.setName(a.getName());
                        achivement.setDetail(a.getDetail());
                        achivement.setMaxCount(a.getMaxCount());
                        achivement.setMoney(a.getMoney());
                        player.playerTask.achivements.add(achivement);
                    }

                    List<AchivementTemplate> listAchivements = AchiveManager.getInstance().getList();
                    if (dataArray.size() < listAchivements.size()) { //add thêm nhiệm vụ khi có nhiệm vụ mới
                        for (int i = dataArray.size(); i < listAchivements.size(); i++) {
                            AchivementTemplate a = AchiveManager.getInstance().findByID(i);
                            Achivement achivement = new Achivement();
                            if (a != null) {
                                achivement.setId(a.getId());
                                achivement.setCount(0);
                                achivement.setFinish(false);
                                achivement.setReceive(false);
                                achivement.setName(a.getName());
                                achivement.setDetail(a.getDetail());
                                achivement.setMaxCount(a.getMaxCount());
                                achivement.setMoney(a.getMoney());
                                player.playerTask.achivements.add(achivement);
                            }
                        }
                    }
                    dataArray.clear();

                    //data trứng bư
                    dataObject = (JSONObject) jv.parse(rs.getString("data_mabu_egg"));
                    Object createTime = dataObject.get("create_time");
                    if (createTime != null) {
                        player.mabuEgg = new MabuEgg(player, Long.parseLong(String.valueOf(createTime)),
                                Long.parseLong(String.valueOf(dataObject.get("time_done"))));
                    }
                    dataObject.clear();

                    //data Linhthu
                    dataObject = (JSONObject) jv.parse(rs.getString("data_egg_linhthu"));
                    Object lt = dataObject.get("create_time");
                    if (lt != null) {
                        player.egglinhthu = new EggLinhThu(player, Long.parseLong(String.valueOf(lt)),
                                Long.parseLong(String.valueOf(dataObject.get("time_done"))));
                    }
                    dataObject.clear();

                    //data bill
                    dataObject = (JSONObject) jv.parse(rs.getString("data_bill_egg"));
                    Object ct = dataObject.get("create_time");
                    if (ct != null) {
                        player.billEgg = new BillEgg(player, Long.parseLong(String.valueOf(ct)),
                                Long.parseLong(String.valueOf(dataObject.get("time_done"))));
                    }
                    dataObject.clear();

                    //data bùa
                    dataArray = (JSONArray) jv.parse(rs.getString("data_charm"));
                    player.charms.tdTriTue = Long.parseLong(dataArray.get(0).toString());
                    player.charms.tdManhMe = Long.parseLong(dataArray.get(1).toString());
                    player.charms.tdDaTrau = Long.parseLong(dataArray.get(2).toString());
                    player.charms.tdOaiHung = Long.parseLong(dataArray.get(3).toString());
                    player.charms.tdBatTu = Long.parseLong(dataArray.get(4).toString());
                    player.charms.tdDeoDai = Long.parseLong(dataArray.get(5).toString());
                    player.charms.tdThuHut = Long.parseLong(dataArray.get(6).toString());
                    player.charms.tdDeTu = Long.parseLong(dataArray.get(7).toString());
                    player.charms.tdTriTue3 = Long.parseLong(dataArray.get(8).toString());
                    player.charms.tdTriTue4 = Long.parseLong(dataArray.get(9).toString());
                    if (dataArray.size() >= 11) {
                        player.charms.tdDeTuMabu = Long.parseLong(dataArray.get(10).toString());
                    }
                    dataArray.clear();

                    //data skill
                    dataArray = (JSONArray) jv.parse(rs.getString("skills"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        JSONArray skillTemp = (JSONArray) jv.parse(String.valueOf(dataArray.get(i)));
                        int tempId = Integer.parseInt(skillTemp.get(0).toString());
                        byte point = Byte.parseByte(skillTemp.get(2).toString());
                        Skill skill = null;
                        if (point != 0) {
                            skill = SkillUtil.createSkill(tempId, point);
                        } else {
                            skill = SkillUtil.createSkillLevel0(tempId);
                        }
                        skill.lastTimeUseThisSkill = Long.parseLong(skillTemp.get(1).toString());
                        player.playerSkill.skills.add(skill);
                        skillTemp.clear();
                    }
                    dataArray.clear();

                    //data skill shortcut
                    dataArray = (JSONArray) jv.parse(rs.getString("skills_shortcut"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        player.playerSkill.skillShortCut[i] = Byte.parseByte(String.valueOf(dataArray.get(i)));
                    }
                    for (int i : player.playerSkill.skillShortCut) {
                        if (player.playerSkill.getSkillbyId(i) != null && player.playerSkill.getSkillbyId(i).damage > 0) {
                            player.playerSkill.skillSelect = player.playerSkill.getSkillbyId(i);
                            break;
                        }
                    }
                    if (player.playerSkill.skillSelect == null) {
                        player.playerSkill.skillSelect = player.playerSkill.getSkillbyId(player.gender == ConstPlayer.TRAI_DAT
                                ? Skill.DRAGON : (player.gender == ConstPlayer.NAMEC ? Skill.DEMON : Skill.GALICK));
                    }
                    dataArray.clear();

                    Gson gson = new Gson();
                    List<Card> cards = gson.fromJson(rs.getString("collection_book"), new TypeToken<List<Card>>() {
                    }.getType());

                    CollectionBook book = new CollectionBook(player);
                    if (cards != null) {
                        book.setCards(cards);
                    } else {
                        book.setCards(new ArrayList<>());
                    }
                    book.init();
                    player.setCollectionBook(book);
                    List<Item> itemsBody = player.inventory.itemsBody;
                    while (itemsBody.size() < 11) {
                        itemsBody.add(ItemService.gI().createItemNull());
                    }

                    if (itemsBody.get(9).isNotNullItem()) {
                        MiniPet.callMiniPet(player, (player.inventory.itemsBody.get(9).template.id));
                    }
                    if (itemsBody.get(10).isNotNullItem()) {
                        PetFollow pet = PetFollowManager.gI().findByID(itemsBody.get(10).getId());
                        player.setPetFollow(pet);
                    }

                    player.canGeFirstTimeLogin = false;
                    player.firstTimeLogin = rs.getTimestamp("firstTimeLogin");

                    dataArray = (JSONArray) JSONValue.parse(rs.getString("buy_limit"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        player.buyLimit[i] = Byte.parseByte(dataArray.get(i).toString());
                    }

                    dataArray = (JSONArray) JSONValue.parse(rs.getString("reward_limit"));

                    player.rewardLimit = new byte[dataArray.size()];
                    for (int i = 0; i < dataArray.size(); i++) {
                        player.rewardLimit[i] = Byte.parseByte(dataArray.get(i).toString());
                    }

                    //dhvt23
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("challenge"));
                    player.goldChallenge = Integer.parseInt(dataArray.get(0).toString());
                    player.levelWoodChest = Integer.parseInt(dataArray.get(1).toString());
                    player.receivedWoodChest = Integer.parseInt(dataArray.get(2).toString()) == 1;
                    dataArray.clear();

                    PlayerService.gI().dailyLogin(player);

                    //data pet
                    dataObject = (JSONObject) jv.parse(rs.getString("pet_info"));
                    if (!String.valueOf(dataObject).equals("{}")) {
                        Pet pet = new Pet(player);
                        pet.id = -player.id;
                        pet.gender = Byte.parseByte(String.valueOf(dataObject.get("gender")));
                        pet.typePet = Byte.parseByte(String.valueOf(dataObject.get("is_mabu")));
                        pet.name = String.valueOf(dataObject.get("name"));
                        player.fusion.typeFusion = Byte.parseByte(String.valueOf(dataObject.get("type_fusion")));
                        player.fusion.lastTimeFusion = System.currentTimeMillis() - (Fusion.TIME_FUSION - Integer.parseInt(String.valueOf(dataObject.get("left_fusion"))));
                        pet.status = Byte.parseByte(String.valueOf(dataObject.get("status")));
                        try {
                            pet.setLever(Integer.parseInt(String.valueOf(dataObject.get("level"))));
                        } catch (Exception e) {
                            pet.setLever(0);
                        }

                        //data chỉ số
                        dataObject = (JSONObject) jv.parse(rs.getString("pet_point"));
                        pet.nPoint.stamina = Short.parseShort(String.valueOf(dataObject.get("stamina")));
                        pet.nPoint.maxStamina = Short.parseShort(String.valueOf(dataObject.get("max_stamina")));
                        pet.nPoint.hpg = Integer.parseInt(String.valueOf(dataObject.get("hpg")));
                        pet.nPoint.mpg = Integer.parseInt(String.valueOf(dataObject.get("mpg")));
                        pet.nPoint.dameg = Integer.parseInt(String.valueOf(dataObject.get("damg")));
                        pet.nPoint.defg = Integer.parseInt(String.valueOf(dataObject.get("defg")));
                        pet.nPoint.critg = Integer.parseInt(String.valueOf(dataObject.get("critg")));
                        pet.nPoint.power = Long.parseLong(String.valueOf(dataObject.get("power")));
                        pet.nPoint.tiemNang = Long.parseLong(String.valueOf(dataObject.get("tiem_nang")));
                        pet.nPoint.limitPower = Byte.parseByte(String.valueOf(dataObject.get("limit_power")));
                        int hp = Integer.parseInt(String.valueOf(dataObject.get("hp")));
                        int mp = Integer.parseInt(String.valueOf(dataObject.get("mp")));

                        //data body
                        dataArray = (JSONArray) jv.parse(rs.getString("pet_body"));
                        for (int i = 0; i < dataArray.size(); i++) {
                            dataObject = (JSONObject) dataArray.get(i);
                            Item item = null;
                            short tempId = Short.parseShort(String.valueOf(dataObject.get("temp_id")));
                            if (tempId != -1) {
                                item = ItemService.gI().createNewItem(tempId, Integer.parseInt(String.valueOf(dataObject.get("quantity"))));
                                JSONArray options = (JSONArray) dataObject.get("option");
                                for (int j = 0; j < options.size(); j++) {
                                    JSONArray opt = (JSONArray) options.get(j);
                                    item.itemOptions.add(new ItemOption(Integer.parseInt(String.valueOf(opt.get(0))),
                                            Integer.parseInt(String.valueOf(opt.get(1)))));
                                }
                                item.createTime = Long.parseLong(String.valueOf(dataObject.get("create_time")));
                                if (ItemService.gI().isOutOfDateTime(item)) {
                                    item = ItemService.gI().createItemNull();
                                }
                            } else {
                                item = ItemService.gI().createItemNull();
                            }
                            pet.inventory.itemsBody.add(item);
                        }

                        if (dataArray.size() < 8) {
                            Item item = ItemService.gI().createItemNull();
                            pet.inventory.itemsBody.add(item);
                        }

                        //data skills
                        dataArray = (JSONArray) jv.parse(rs.getString("pet_skill"));
                        for (int i = 0; i < dataArray.size(); i++) {
                            JSONArray skillTemp = (JSONArray) dataArray.get(i);
                            int tempId = Integer.parseInt(String.valueOf(skillTemp.get(0)));
                            byte point = Byte.parseByte(String.valueOf(skillTemp.get(1)));
                            Skill skill = null;
                            if (point != 0) {
                                skill = SkillUtil.createSkill(tempId, point);
                            } else {
                                skill = SkillUtil.createSkillLevel0(tempId);
                            }
                            switch (skill.template.id) {
                                case Skill.KAMEJOKO:
                                case Skill.MASENKO:
                                case Skill.ANTOMIC:
                                    skill.coolDown = 1000;
                                    break;
                            }
                            pet.playerSkill.skills.add(skill);
                        }
                        if (pet.playerSkill.skills.size() < 5) {
                            pet.playerSkill.skills.add(4, SkillUtil.createSkillLevel0(-1));
                        }
                        pet.nPoint.hp = hp;
                        pet.nPoint.mp = mp;
//                    pet.nPoint.calPoint();
                        player.pet = pet;
                    }
//                    if (session.ruby > 0) {
//                        player.inventory.ruby += session.ruby;
//                        player.playerTask.achivements.get(ConstAchive.LAN_DAU_NAP_NGOC).count += session.ruby;
//                        PlayerDAO.subRuby(player, session.userId, session.ruby);
//                    }
                    player.nPoint.hp = plHp;
                    player.nPoint.mp = plMp;
                    session.player = player;
                    PreparedStatement ps2 = connection.prepareStatement("update account set last_time_login = ?, ip_address = ? where id = ?");
                    ps2.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    ps2.setString(2, session.ipAddress);
                    ps2.setInt(3, session.userId);
                    ps2.executeUpdate();
                    ps2.close();
                    return player;
                }
            } finally {
                rs.close();
                ps.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            session.dataLoadFailed = true;
        }
        return null;
    }

}
