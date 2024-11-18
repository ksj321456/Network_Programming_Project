import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class DrawingServer extends JFrame {
    private static final int PORT = 12345;
    private final List<ClientHandler> clients = new ArrayList<>();
    private JTextArea t_display;

    public DrawingServer() {

        t_display = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(t_display);
        add(scrollPane);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 500);
        setTitle("서버 GUI");
    }

    public void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            printDisplay("서버 시작");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler extends Thread {
        private final Socket socket;
        private ObjectOutputStream out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                //ObjectOutputStream을 ObjectInputStream보다 먼저 생성해야 함. 순서 바뀌면 데드락 발생.
                //현재 이 소켓이, ObjectInputStream을 생성하기 전에 헤더 정보를 수신하면 ObjectInputStream은 스트림 헤더를 기대하지 않았기 때문에 블록 상태에 빠지기때문.
                out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                //out.flush();
                ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));


                SketchingData data;
                while ((data = (SketchingData) in.readObject()) != null) {
                    //채팅 메시지를 받았을 때
                    //printDisplay("클라이언트로부터 데이터 수신");
                    if (data.getMode() == SketchingData.LINE) {
                        Line line = data.getLine();
                        //printDisplay("그리기 좌표: " + line.getX1() + ", " + line.getY1() + ", " + line.getX2() + ", " + line.getY2());
                        broadcast(data, this);
                    }
                    //스케치 데이터를 받았을 때
                    else if (data.getMode() == SketchingData.CHAT) {
                        printDisplay("채팅 메시지: " + data.getMessage());
                        broadcastOthers(data, this);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void broadcast(SketchingData data, ClientHandler sender) {
            for (ClientHandler client : clients)
                client.sendData(data);

        }

        private void broadcastOthers(SketchingData data, ClientHandler sender) {
            for (ClientHandler client : clients) {
                if (client != sender) {  // 전송자를 제외하고 브로드캐스팅
                    client.sendData(data);
                }
            }
        }

        private void sendData(SketchingData data) {
            try {
                out.writeObject(data);
                out.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void printDisplay(String msg) {
        t_display.append(msg + "\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());
    }

    public static void main(String[] args) {
        DrawingServer drawingServer = new DrawingServer();
        drawingServer.startServer();
    }
}