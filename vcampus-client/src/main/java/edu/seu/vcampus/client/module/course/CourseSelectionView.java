package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.SelectionBatchInfo;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;

/**
 * 选课系统客户端根页面。
 *
 * 负责在选课中心首页和具体批次页面之间切换。
 */
final class CourseSelectionView extends JPanel {

    private static final String CENTER = "center";
    private static final String BATCH = "batch";

    private final ClientContext context;

    private final CardLayout cardLayout =
        new CardLayout();

    private final CourseCenterPanel centerPanel;

    private final JPanel batchContainer =
        new JPanel(
            new BorderLayout());

    CourseSelectionView(
        ClientContext context) {

        this.context = context;

        setLayout(cardLayout);

        /*
         * 选课中心首页。
         *
         * 当用户点击某个批次的“进入”按钮时，
         * CourseCenterPanel 会调用 openBatch。
         */
        centerPanel =
            new CourseCenterPanel(
                context,
                this::openBatch);

        add(
            centerPanel,
            CENTER);

        add(
            batchContainer,
            BATCH);

        cardLayout.show(
            this,
            CENTER);
    }

    /**
     * 打开具体选课批次。
     */
    private void openBatch(
        SelectionBatchInfo batch) {

        /*
         * 每次进入批次时，
         * 根据当前批次重新创建页面。
         */
        batchContainer.removeAll();

        batchContainer.add(
            new CourseBatchPanel(
                context,
                batch,
                this::openCenter),
            BorderLayout.CENTER);

        batchContainer.revalidate();
        batchContainer.repaint();

        cardLayout.show(
            this,
            BATCH);
    }

    /**
     * 返回选课中心首页。
     */
    private void openCenter() {

        cardLayout.show(
            this,
            CENTER);
    }
}
