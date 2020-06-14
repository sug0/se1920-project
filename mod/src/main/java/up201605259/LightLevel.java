package up201605259;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Mod(modid = LightLevel.MODID, version = LightLevel.VERSION)
public class LightLevel {
    public static final String MODID = "selightlevel";
    public static final String VERSION = "1.0";

    private static Logger LOGGER = LogManager.getLogger(MODID);
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        LOGGER.info("The bomb has been planted.");
        MinecraftForge.EVENT_BUS.register(new LightLevel.Handler());
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent e) {
        LOGGER.info("The bomb has been defused.");
        MQTT.shutdown();
    }

    public static class MQTT {
        private static MqttClient INSTANCE = null;

        public static void publish(String topic, String payload) {
            try {
                if (INSTANCE == null) {
                    INSTANCE = new MqttClient(
                        "tcp://broker.hivemq.com:1883",
                        "sugo-12345",
                        new MemoryPersistence()
                    );
                    MqttConnectOptions opts = new MqttConnectOptions();
                    opts.setConnectionTimeout(60);
                    LOGGER.info("Connecting to MQTT...");
                    INSTANCE.connect(opts);
                    INSTANCE.subscribe("minecraft/fcup/accelerometer-data", new IMqttMessageListener() {
                        final Pattern pt = Pattern.compile("\\[(-?\\d+),\\s?(-?\\d+),\\s?(-?\\d+)\\]");

                        @Override
                        public void messageArrived(String topic, MqttMessage message) {
                            final Minecraft mc = Minecraft.getMinecraft();
                            final EntityPlayerSP player = mc.player;
                            if (player == null) {
                                return;
                            }

                            final Matcher m = pt.matcher(message.toString());
                            if (!m.find()) {
                                return;
                            }

                            final int valUp = Integer.parseInt(m.group(2));
                            final double valDown = Double.parseDouble(m.group(3));

                            switch (valUp) {
                            case 9:
                            case 10:
                            case -9:
                            case -10:
                                break;
                            default:
                                final EnumFacing facing = player.getAdjustedHorizontalFacing();
                                switch (facing) {
                                case NORTH:
                                    player.setPositionAndUpdate(player.posX, player.posY, player.posZ - valDown);
                                    break;
                                case SOUTH:
                                    player.setPositionAndUpdate(player.posX, player.posY, player.posZ + valDown);
                                    break;
                                case EAST:
                                    player.setPositionAndUpdate(player.posX + valDown, player.posY, player.posZ);
                                    break;
                                case WEST:
                                    player.setPositionAndUpdate(player.posX - valDown, player.posY, player.posZ);
                                    break;
                                }
                            }
                        }
                    });
                }
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to connect to MQTT broker: %s", e));
                return;
            }

            try {
                MqttMessage message = new MqttMessage();
                message.setRetained(false);
                message.setPayload(payload.getBytes());
                message.setQos(0);
                INSTANCE.publish(topic, message);
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to publish message to MQTT broker: %s", e));
            }
        }

        public static void shutdown() {
            if (INSTANCE != null) {
                try {
                    INSTANCE.disconnect();
                    INSTANCE = null;
                } catch (Exception e) {
                    LOGGER.warn(String.format("Failed to stop MQTT client: %s", e));
                }
            }
        }
    }

    public static class Handler {
        public static final int TICKS_AFTER = 5;
        private int tickCounter;

        public Handler() {
            tickCounter = 0;
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent e) {
            final Minecraft mc = Minecraft.getMinecraft();

            final EntityPlayerSP player = mc.player;
            final World world = mc.world;

            final GuiScreen gui = mc.currentScreen;
            final boolean gamePaused = (gui != null && gui.doesGuiPauseGame());

            if (gamePaused || world == null || player == null) {
                return;
            }

            if (tickCounter == 0) {
                sendLightLevel(player, world);
            }

            tickCounter = (tickCounter + 1) % TICKS_AFTER;
        }

        private static void sendLightLevel(EntityPlayerSP player, World world) {
            BlockPos p = new BlockPos(
                Math.floor(player.posX),
                Math.floor(player.posY),
                Math.floor(player.posZ)
            );

            int lightLevel = world.getLight(p);

            MQTT.publish(
                "minecraft/fcup/light-level",
                String.format("{\"light\":%d}", lightLevel)
            );
        }
    }
}
