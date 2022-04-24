package aqua.blatt1.client;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

public class SnapshotController implements ActionListener {
    private final Component parent;
    private final TankModel tankModel;

    public SnapshotController(Component parent, TankModel tankModel) {
        this.parent = parent;
        this.tankModel = tankModel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tankModel.initiateSnapshot(TankModel.RecordingState.BOTH);
        tankModel.initCollector();
    }
}
