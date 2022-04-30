package aqua.blatt1.client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ToggleController implements ActionListener {
    private final Component parent;
    private final TankModel tankModel;

    public ToggleController(Component parent, TankModel tankModel) {
        this.parent = parent;
        this.tankModel = tankModel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tankModel.locateFishGlobally(e.getActionCommand());
    }
}
