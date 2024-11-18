import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.*;
import java.net.Socket;

public class DrawingClient extends JFrame {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private Socket socket;
    private ObjectOutputStream out;
    private DrawPanel drawPanel;    // 그림판 Panel
    private ChatingListPanel chatingListPanel; // 채팅창 Panel
    private String userId; // 사용자 ID
    private LeftUserPanel leftUserPanel;
    private RightUserPanel rightUserPanel;
    private DrawingSetting drawingSetting;

    private boolean isDrawing = false;      // 그림 그리고 있는지 확인
    private Point lastPoint = null;  // 마지막 좌표

    public DrawingClient(String userId) {
        this.userId = userId;
        setResizable(false);
        setTitle("Hansung Sketching");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        drawPanel = new DrawPanel();
        leftUserPanel = new LeftUserPanel();
        rightUserPanel = new RightUserPanel();
        chatingListPanel = new ChatingListPanel();
        drawingSetting = new DrawingSetting();
        InputPanel inputPanel = new InputPanel(this);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(drawPanel, BorderLayout.CENTER);
        centerPanel.add(leftUserPanel, BorderLayout.WEST);
        centerPanel.add(rightUserPanel, BorderLayout.EAST);
        centerPanel.add(drawingSetting, BorderLayout.NORTH);

        add(centerPanel);


        add(inputPanel, BorderLayout.SOUTH);
        add(chatingListPanel, BorderLayout.EAST);
        setVisible(true);
        connectToServer();

    }

    // 그리기를 실행하는 Thread
    private class DrawingThread extends Thread implements MouseMotionListener, MouseListener {
        private MouseEvent currentEvent;

        @Override
        public void run() {
            drawPanel.addMouseMotionListener(this);
            drawPanel.addMouseListener(this);
        }

        // MouseMotionListener 구현
        @Override
        public void mouseDragged(MouseEvent e) {
            if (isDrawing && lastPoint != null) { // 그리기 상태일 때만 좌표를 전송
                Line line = new Line(lastPoint.x, lastPoint.y, e.getX(), e.getY());
                SketchingData sketchingData = new SketchingData(SketchingData.LINE, line);

                // 서버에 Line 객체 전송
                try {
                    out.writeObject(sketchingData);
                    out.flush();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

                // 마지막 좌표를 현재 좌표로 업데이트
                lastPoint = new Point(e.getX(), e.getY());

            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {

        }

        @Override
        public void mouseClicked(MouseEvent e) {

        }

        // MouseListener 구현
        @Override
        public void mousePressed(MouseEvent e) {
            isDrawing = true; // 그리기 시작
            lastPoint = new Point(e.getX(), e.getY()); // 현재 좌표 저장
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            isDrawing = false; // 그리기 상태 종료
        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    }


    public void sendMessage(String message) {
        String fullMessage = userId + ": " + message;
        SketchingData chatData = new SketchingData(SketchingData.CHAT, fullMessage);
        chatingListPanel.addMessage("나: " + message);

        try {
            out.writeObject(chatData);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            //out.flush();

            //순서: 그리기를 실행하는 스레드 실행 후 -> 데이터 수신 스레드 실행. 역순으로하게되면 채팅 전송하기전까지 그림을 그릴 수 없음.
            DrawingThread drawingThread = new DrawingThread();
            drawingThread.start();

            Thread sendCoordsThread = new ReceiveThread(socket);
            sendCoordsThread.start(); //ObjectOutputStream을 ObjectInputStream보다 먼저 생성해야 함. 미준수시 데드락 발생 가능성 있음.


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 메세지를 수신하는 스레드
    private class ReceiveThread extends Thread {
        private ObjectInputStream in;

        public ReceiveThread(Socket socket) throws IOException {
            in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
        }

        @Override
        public void run() {
            SketchingData data;
            try {
                while ((data = (SketchingData) in.readObject()) != null) {
                    // 그리기 모드를 받았을 때
                    if (data.getMode() == SketchingData.LINE) {
                        Line line = data.getLine();
                        drawPanel.addLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
                    }
                    // 채팅 모드를 받았을 때
                    else if (data.getMode() == SketchingData.CHAT) {
                        chatingListPanel.addMessage(data.getMessage());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        String userId = JOptionPane.showInputDialog("사용자 ID를 입력하세요:"); // 다이얼로그로 사용자 ID 입력
        DrawingClient drawingClient = new DrawingClient(userId);
        //drawingClient.connectToServer();
    }
}