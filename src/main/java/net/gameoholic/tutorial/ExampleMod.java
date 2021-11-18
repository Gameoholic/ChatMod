package net.gameoholic.tutorial;


import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.api.Event;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

@Mod(ExampleMod.MOD_ID)
public class ExampleMod {
    public static final String MOD_ID = "tutorial";
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();
    private static long lastModified;
    public static Queue<String> commandsQueue = new LinkedList<String>();
    public ExampleMod() throws IOException {
        final var modBus = FMLJavaModLoadingContext.get().getModEventBus();

        MinecraftForge.EVENT_BUS.addListener(this::chatMessage); // this manually registers the event directly to the mod bus
        MinecraftForge.EVENT_BUS.addListener(this::clientTickEvent); // this manually registers the event directly to the mod bus

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress host = InetAddress.getByName("localhost");
                    Selector selector = Selector.open();
                    ServerSocketChannel serverSocketChannel =
                            ServerSocketChannel.open();
                    serverSocketChannel.configureBlocking(false);
                    serverSocketChannel.bind(new InetSocketAddress(host, 6000));
                    serverSocketChannel.register(selector, SelectionKey.
                            OP_ACCEPT);
                    SelectionKey key = null;
                    while (true) {
                        if (selector.select() <= 0)
                            continue;
                        Set<SelectionKey> selectedKeys = selector.selectedKeys();
                        Iterator<SelectionKey> iterator = selectedKeys.iterator();
                        while (iterator.hasNext()) {
                            key = (SelectionKey) iterator.next();
                            iterator.remove();
                            //Minecraft.getInstance().player.sendMessage(new TextComponent("troll"), Util.NIL_UUID);
                            try {
                                if (key.isAcceptable()) {
                                    SocketChannel sc = serverSocketChannel.accept();
                                    sc.configureBlocking(false);
                                    sc.register(selector, SelectionKey.
                                            OP_READ);

                                    Minecraft.getInstance().player.sendMessage(new TextComponent("Connection accepted: " + sc.getLocalAddress()));
                                }
                                if (key.isReadable()) {
                                    SocketChannel sc = (SocketChannel) key.channel();
                                    ByteBuffer bb = ByteBuffer.allocate(1024);
                                    sc.read(bb);
                                    String result = new String(bb.array()).trim();
                                    if (result.length() <= 0) {
                                        sc.close();
                                        Minecraft.
                                        Minecraft.getInstance().player.sendMessage(new TextComponent("connection closed."), Util.NIL_UUID);
                                    }
                                    else {
                                        Minecraft.getInstance().player.sendMessage(new TextComponent("Message received: " + result), Util.NIL_UUID);
                                        if (result.startsWith("/")) {
                                            commandsQueue.add(result);
                                            Minecraft.getInstance().player.sendMessage(new TextComponent(result), Util.NIL_UUID);
                                        }
                                    }
                                }
                            }
                            catch (Exception e) {
                                Minecraft.getInstance().player.sendMessage(new TextComponent("ERROR 1: " + e.getMessage()), Util.NIL_UUID);
                                selector.close();
                                Minecraft.getInstance().player.sendMessage(new TextComponent("Closed connection!"), Util.NIL_UUID);
                            }
                        }
                    }
                }
                catch (Exception e) {
                    Minecraft.getInstance().player.sendMessage(new TextComponent("ERROR 2: " + e.getMessage()), Util.NIL_UUID);
                }

            }
        });
        t1.start();


    }


    public void chatMessage(ClientChatReceivedEvent event) {
        var msg = event.getMessage();
        try {

            String content = msg.getString();

            ByteBuffer buffer = StandardCharsets.UTF_8.encode(content);

            String encodedString = StandardCharsets.UTF_8.decode(buffer).toString();

            Socket s = new Socket("localhost", 4999);
            PrintWriter pr = new PrintWriter(s.getOutputStream());
            pr.println(encodedString);
            pr.flush();
        } catch (Exception e) {
            Minecraft.getInstance().player.sendMessage(new TextComponent("ERROR 3: " + e.getMessage()), Util.NIL_UUID);
        }

    }

    public void clientTickEvent(TickEvent.PlayerTickEvent event) {

        if (!commandsQueue.isEmpty()) {
            String msg = commandsQueue.remove();
            //Send message in chat:
            Minecraft.getInstance().getConnection().send(new ServerboundChatPacket(msg));

            //Minecraft.getInstance().player.sendMessage(new TextComponent("/gc " + msg), Util.NIL_UUID);
        }
    }

}
