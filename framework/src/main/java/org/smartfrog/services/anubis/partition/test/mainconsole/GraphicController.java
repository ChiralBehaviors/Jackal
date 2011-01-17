package org.smartfrog.services.anubis.partition.test.mainconsole;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.smartfrog.services.anubis.Anubis;
import org.smartfrog.services.anubis.partition.test.colors.ColorAllocator;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;

import com.hellblazer.anubis.annotations.Deployed;

public class GraphicController extends Controller {
    private ColorAllocator colorAllocator = new ColorAllocator();
    private MainConsoleFrame consoleFrame;

    public synchronized void asymPartition(String nodeStr) {

        StringTokenizer tokens = new StringTokenizer(nodeStr);
        BitView partition = new BitView();
        String token = "";

        if (tokens.countTokens() == 0) {
            return;
        }

        try {
            while (tokens.hasMoreTokens()) {
                token = tokens.nextToken();
                partition.add(new Integer(token).intValue());
            }
        } catch (NumberFormatException ex) {
            consoleFrame.inputError("Unknown: " + token);
        }
        asymPartition(partition);
    }

    @Override
    @Deployed
    public synchronized void deploy() throws IOException {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (UnsupportedLookAndFeelException e) {
            throw new IllegalStateException(e);
        }
        super.deploy();
        consoleFrame = new MainConsoleFrame(this);
        consoleFrame.setTitle("Partition Manager Test Controller - "
                              + Anubis.version);
        consoleFrame.initialiseTiming(heartbeatInterval, heartbeatTimeout);
    }

    @Override
    public long getHeartbeatInterval() {
        heartbeatInterval = consoleFrame.getInterval();
        return super.getHeartbeatInterval();
    }

    @Override
    public long getHeartbeatTimeout() {
        heartbeatTimeout = consoleFrame.getTimeout();
        return super.getHeartbeatTimeout();
    }

    @Override
    public void removeNode(NodeData nodeData) {
        consoleFrame.removeNode((GraphicNodeData) nodeData);
    }

    @Override
    protected void addNode(HeartbeatMsg hb, NodeData nodeData) {
        consoleFrame.addNode((GraphicNodeData) nodeData);
        super.addNode(hb, nodeData);
    }

    @PostConstruct
    public synchronized void start() {
        consoleFrame.setVisible(true);
    }

    @Override
    @PreDestroy
    public synchronized void terminate() {
        super.terminate();
        consoleFrame.setVisible(false);
        consoleFrame.dispose();
        consoleFrame = null;
        System.exit(0);
    }

    public synchronized void symPartition(String nodeStr) {

        StringTokenizer tokens = new StringTokenizer(nodeStr);
        BitView partition = new BitView();
        String token = "";

        if (tokens.countTokens() == 0) {
            return;
        }

        try {
            while (tokens.hasMoreTokens()) {
                token = tokens.nextToken();
                partition.add(new Integer(token).intValue());
            }
        } catch (NumberFormatException ex) {
            consoleFrame.inputError("Unknown: " + token);
            return;
        }

        symPartition(partition);
    }

    protected NodeData createNode(HeartbeatMsg hb) {
        NodeData nodeData;
        nodeData = new GraphicNodeData(hb, colorAllocator, this);
        return nodeData;
    }
}
