import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// 그림판 클래스
public class DrawPanel extends JPanel {
    private List<Line> lines = new ArrayList<>();  // 선을 저장할 리스트

    public DrawPanel() {
        setBackground(Color.WHITE);
    }

    // 점 대신 선을 추가
    // 현재 한 스레드에서 paintComponent 메서드의 for문에서 lines 리스트에 접근중.
    // 이때, 다른 스레드에서 addLine 메서드를 통해 lines 리스트에 접근하여 수정하게되면 ConcurrentModificationException이 발생할 수 있음.
    // so, addLine 메서드와 paintComponent 메서드에 synchronized 키워드를 추가하여 lines리스트에 대한 접근을 동기화 처리를 해줌.
    public synchronized void addLine(int x1, int y1, int x2, int y2) {
        lines.add(new Line(x1, y1, x2, y2));
        repaint();  // 선이 추가될 때마다 repaint 호출
    }

    @Override
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);

        // 저장된 선들을 그리기
        for (Line line : lines) {
            g.drawLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
        }
    }
}