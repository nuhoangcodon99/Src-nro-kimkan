/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nro.resources;

import nro.consts.Cmd;
import nro.data.DataGame;
import nro.resources.entity.EffectData;
import nro.resources.entity.ImageByName;
import nro.resources.entity.MobData;
import nro.server.io.Message;
import nro.server.io.Session;
import nro.utils.FileUtils;
import nro.utils.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import nro.utils.FileIO;

/**
 *
 * @Build by Arriety
 */
public class Resources {

    private static final Resources instance = new Resources();

    private static int vsRes = 19061997;

    public static Resources getInstance() {
        return instance;
    }

    private final List<AbsResources> resourceses;

    public Resources() {
        resourceses = new ArrayList<>();
        resourceses.add(new RNormal());
//        resourceses.add(new RSpecial());
    }

    public void init() {
        for (AbsResources res : resourceses) {
            res.init();
        }
    }

    public byte[] readAllBytes(byte type, String path) {
        AbsResources resources = find(type);
        return resources.readAllBytes(path);
    }

    public List<String> readAllLines(byte type, String path) {
        AbsResources resources = find(type);
        return resources.readAllLines(path);
    }

    public AbsResources find(int type) {// debug 
        if (resourceses.size() >= 2) {
            if (type == 5) {
                return resourceses.get(1);
            }
        }
        return resourceses.get(0);
    }

    public void downloadResources(Session session, Message ms) {
        try {
            byte type = ms.reader().readByte();
            if (type == 1) {
                AbsResources res = find(session.typeClient);
                if (res != null) {
                    File root = new File(res.getFolder(), "data/" + session.zoomLevel);
                    ArrayList<File> datas = new ArrayList<>();
                    FileUtils.addPath(datas, root);
                    sendNumberOfFiles(session, (short) datas.size());
                    for (File file : datas) {
                        fileTransfer(session, root, file);
                    }
                    fileTransferCompleted(session);
                }
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(DataGame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendNumberOfFiles(Session session, short size) {
        Message msg;
        try {
            msg = new Message(Cmd.GET_IMAGE_SOURCE);
            msg.writer().writeByte(1);
            msg.writer().writeShort(size);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void fileTransferCompleted(Session session) {
        AbsResources res = find(session.typeClient);
        if (res != null) {
            int[] version = res.getDataVersion();
            Message msg;
            try {
                msg = new Message(Cmd.GET_IMAGE_SOURCE);
                msg.writer().writeByte(3);
                msg.writer().writeInt(version[session.zoomLevel - 1]);
                session.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
                Log.error(DataGame.class, e);
            }
        }
    }

    public void sendResVersion(Session session) {
        try {
            AbsResources res = find(session.typeClient);
            if (res != null) {
                int[] version = res.getDataVersion();
                Message mss = new Message(Cmd.GET_IMAGE_SOURCE);
                DataOutputStream ds = mss.writer();
                ds.writeByte(0);
                ds.writeInt(version[session.zoomLevel - 1]);
                ds.flush();
                session.sendMessage(mss);
                mss.cleanup();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void fileTransfer(Session session, File root, File file) {
        try {
            String strPath = file.getPath();
            strPath = strPath.replace(root.getPath(), "");
            strPath = FileUtils.cutPng(strPath);
            strPath = strPath.replace("\\", "/");
            Message mss = new Message(Cmd.GET_IMAGE_SOURCE);
            DataOutputStream ds = mss.writer();
            ds.writeByte(2);
            ds.writeUTF(strPath);
            byte[] ab = Files.readAllBytes(file.toPath());
            ds.writeInt(ab.length);
            ds.write(ab);
            ds.flush();
            session.sendMessage(mss);
            mss.cleanup();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void downloadIconData(Session session, int id) {
        try {
            AbsResources res = find(session.typeClient);
            if (res != null) {
                byte[] data = res.getRawIconData(session.zoomLevel, id);
                Message msg = new Message(Cmd.REQUEST_ICON);
                msg.writer().writeInt(id);
                msg.writer().writeInt(data.length);
                msg.writer().write(data);
                session.sendMessage(msg);
                msg.cleanup();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void downloadBGTemplate(Session session, int id) {
        try {
            AbsResources res = find(session.typeClient);
            if (res != null) {
                byte[] data = res.getRawBGData(session.zoomLevel, id);
                Message msg = new Message(Cmd.BACKGROUND_TEMPLATE);
                msg.writer().writeShort(id);
                msg.writer().writeInt(data.length);
                msg.writer().write(data);
                session.sendMessage(msg);
                msg.cleanup();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendSmallVersion(Session session) {
        try {
            AbsResources res = find(session.typeClient);
            if (res != null) {
                byte[][] smallVersion = res.getSmallVersion();
                byte[] data = smallVersion[session.zoomLevel - 1];
                Message ms = new Message(Cmd.SMALLIMAGE_VERSION);
                ms.writer().writeShort(data.length);
                ms.writer().write(data);
                session.sendMessage(ms);
                ms.cleanup();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendBGVersion(Session session) {
        try {
            AbsResources res = find(session.typeClient);
            if (res != null) {
                byte[][] backgroundVersion = res.getBackgroundVersion();
                byte[] data = backgroundVersion[session.zoomLevel - 1];
                Message ms = new Message(Cmd.BGITEM_VERSION);
                ms.writer().writeShort(data.length);
                ms.writer().write(data);
                session.sendMessage(ms);
                ms.cleanup();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void downloadIBN(Session session, String filename) {
        try {
            AbsResources res = find(session.typeClient);
            ImageByName ibn = res.getIBN(filename);
            if (ibn != null) {
                byte[] data = res.getRawIBNData(session.zoomLevel, filename);
                Message msg = new Message(Cmd.GET_IMG_BY_NAME);
                msg.writer().writeUTF(ibn.getFilename());
                msg.writer().writeByte(ibn.getNFame());
                msg.writer().writeInt(data.length);
                msg.writer().write(data);
                session.sendMessage(msg);
                msg.cleanup();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadMoData(Session session, int id) {
        try {
            AbsResources res = find(session.typeClient);
            if (res != null) {
                MobData mob = res.getMobData(id);
                if (mob != null) {
                    byte[] data = mob.getDataMob();
                    byte[] imgData = res.getRawMobData(session.zoomLevel, id);
                    Message ms = new Message(Cmd.REQUEST_NPCTEMPLATE);
                    DataOutputStream ds = ms.writer();
                    ds.writeByte(mob.getId());
                    ds.writeByte(mob.getType());
                    ds.writeInt(data.length);
                    ds.write(data);
                    ds.writeInt(imgData.length);
                    ds.write(imgData);
                    ds.writeByte(mob.getTypeData());
                    if (mob.getTypeData() == 1 || mob.getTypeData() == 2) {
                        byte[][] frameBoss = mob.getFrameBoss();
                        ds.writeByte(frameBoss.length);
                        for (byte[] frame : frameBoss) {
                            ds.writeByte(frame.length);
                            ds.write(frame);
                        }
                    }
                    ds.flush();
                    session.sendMessage(ms);
                    ms.cleanup();
                }
            }
        } catch (Exception e) {
        }
    }

    public static void sendEffectTemplate(Session session, int id) {
        Message msg;
        try {
            byte[] eff_data = FileIO.readFile("Eff/effdata/x" + session.zoomLevel + "/" + id);
            msg = new Message(-66);
            msg.writer().write(eff_data);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public static void effData(Session session, int id, int... idtemp) {
        int idT = id;
        if (idtemp.length > 0 && idtemp[0] != 0) {
            idT = idtemp[0];
        }
        Message msg;
        try {
            byte[] effData = FileIO.readFile("Eff/effect/x" + session.zoomLevel + "/data/DataEffect_" + idT);
            byte[] effImg = FileIO.readFile("Eff/effect/x" + session.zoomLevel + "/img/ImgEffect_" + idT + ".png");
            msg = new Message(-66);
            msg.writer().writeShort(id);
            msg.writer().writeInt(effData.length);
            msg.writer().write(effData);
            msg.writer().writeByte(0);
            msg.writer().writeInt(effImg.length);
            msg.writer().write(effImg);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public static void requestMobTemplate(Session session, int id) {
        Message msg;
        try {
            byte[] mob = FileIO.readFile("mob/x" + session.zoomLevel + "/" + id);
            if (mob != null) {
                msg = new Message(11);

                if (id < 82) {
                    msg.writer().writeByte(id);
                }

                if (id == 82) {
                    msg.writer().writeByte(0);
                }

                msg.writer().write(mob);
                session.sendMessage(msg);
                msg.cleanup();
            }
        } catch (Exception e) {
            System.err.println("Mob Loi: " + id);
        }
    }

    public void loadEffectData(Session session, int id) {
        AbsResources res = find(session.typeClient);
        if (res != null) {
            try {
                int effId = id;
                if (id == 25) {
                    if (session.player != null && session.player.zone != null) {
                        byte effDragon = session.player.zone.effDragon;
                        if (effDragon != -1) {
                            effId = effDragon;
                            if (effId == 60) {
                                if (!session.isVersionAbove(220)) {
                                    effId = 61;
                                }
                            }
                        }
                    }
                }
                EffectData eff = res.getEffectData(effId);
                if (eff != null) {
                    byte[] data = eff.getData(session.version);
                    byte[] imgData = res.getRawEffectData(session.zoomLevel, effId);
                    Message ms = new Message(Cmd.GET_EFFDATA);
                    DataOutputStream ds = ms.writer();
                    ds.writeShort(id);
                    ds.writeInt(data.length);
                    ds.write(data);
                    if (session.isVersionAbove(220)) {
                        ds.writeByte(eff.getType());
                    }
                    ds.writeInt(imgData.length);
                    ds.write(imgData);
                    ds.flush();
                    session.sendMessage(ms);
                    ms.cleanup();
                }
            } catch (Exception e) {
                Log.error("");
            }
        }
    }
}
