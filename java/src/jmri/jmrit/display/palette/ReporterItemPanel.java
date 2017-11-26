package jmri.jmrit.display.palette;

import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import jmri.NamedBean;
import jmri.jmrit.catalog.NamedIcon;
import jmri.jmrit.display.Editor;
import jmri.jmrit.display.ReporterIcon;
import jmri.jmrit.picker.PickListModel;
import jmri.util.JmriJFrame;
import jmri.util.swing.DrawSquares;
import jmri.util.swing.ImagePanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReporterItemPanel extends TableItemPanel {

    ReporterIcon _reporter;

    public ReporterItemPanel(JmriJFrame parentFrame, String type, String family, PickListModel<jmri.Reporter> model, Editor editor) {
        super(parentFrame, type, family, model, editor);
    }

    @Override
    public void init() {
        if (!_initialized) {
            super.init();
        }
    }

    protected JPanel instructions() {
        JPanel blurb = new JPanel();
        blurb.setLayout(new BoxLayout(blurb, BoxLayout.Y_AXIS));
        blurb.add(Box.createVerticalStrut(ItemPalette.STRUT_SIZE));
        blurb.add(new JLabel(Bundle.getMessage("PickRowReporter")));
        blurb.add(new JLabel(Bundle.getMessage("DragReporter")));
        blurb.add(Box.createVerticalStrut(ItemPalette.STRUT_SIZE));
        JPanel panel = new JPanel();
        panel.add(blurb);
        return panel;
    }

    /**
     * ReporterItemPanel uses familyPanel as Preview pane.
     * TODO override inherited panel class and add makeBgButtonPanel()
     */
    @Override
    protected void initIconFamiliesPanel() {
        _iconFamilyPanel = new JPanel();
        _iconFamilyPanel.setOpaque(true);
        _iconFamilyPanel.setLayout(new BoxLayout(_iconFamilyPanel, BoxLayout.Y_AXIS));
        if (!_update) {
            _iconFamilyPanel.add(instructions());
        }

        // create array of backgrounds
        if (_backgrounds == null) { // reduces load but will not redraw for new size
            _backgrounds = new BufferedImage[5];
            _currentBackground = _editor.getTargetPanel().getBackground(); // start using Panel background color
            _backgrounds[0] = DrawSquares.getImage(500, 100, 20, _currentBackground, _currentBackground);
            for (int i = 1; i <= 3; i++) {
                _backgrounds[i] = DrawSquares.getImage(500, 100, 20, colorChoice[i - 1], colorChoice[i - 1]);
                // choice 0 is not in colorChoice[]
            }
            _backgrounds[4] = DrawSquares.getImage(500, 100, 20, Color.white, _grayColor);
        }
        // _iconPanel = new JPanel();
        // _iconPanel.setOpaque(false);
        // _iconPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black, 1),
        //     Bundle.getMessage("PreviewBorderTitle")));
        // _iconFamilyPanel.add(_iconPanel);
        makeDragIconPanel(1);
        makeDndIconPanel(null, null);
    }

    @Override
    protected void makeBottomPanel(ActionListener doneAction) {
        if (doneAction != null) {
            addUpdateButtonToBottom(doneAction);
        }
        initIconFamiliesPanel();
        add(_iconFamilyPanel);
    }
    
    @Override
    protected void makeDndIconPanel(HashMap<String, NamedIcon> iconMap, String displayKey) {
        if (_update) {
            return;
        }
        _reporter = new ReporterIcon(_editor);
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        JPanel comp;
        try {
            comp = getDragger(new DataFlavor(Editor.POSITIONABLE_FLAVOR));
        } catch (java.lang.ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
            comp = new JPanel();
        }
        comp.setOpaque(false);
        comp.setToolTipText(Bundle.getMessage("ToolTipDragIcon"));
        panel.add(comp);
        panel.revalidate();
        int width = Math.max(100, panel.getPreferredSize().width);
        panel.setPreferredSize(new java.awt.Dimension(width, panel.getPreferredSize().height));
        panel.setToolTipText(Bundle.getMessage("ToolTipDragIcon"));
        _dragIconPanel.add(panel);
        _dragIconPanel.setToolTipText(Bundle.getMessage("ToolTipDragIcon"));
        _dragIconPanel.invalidate();
    }

    protected JPanel makeItemButtonPanel() {
        return new JPanel();
    }

    /**
     * ListSelectionListener action from table.
     */
    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (_table == null) {
            return;
        }
        int row = _table.getSelectedRow();
        log.debug("Table valueChanged: row = {}", row);
        if (row >= 0) {
            if (_updateButton != null) {
                _updateButton.setEnabled(true);
                _updateButton.setToolTipText(null);
            }
            NamedBean bean = getDeviceNamedBean();
            _reporter.setReporter(bean.getDisplayName());
        } else {
            if (_updateButton != null) {
                _updateButton.setEnabled(false);
                _updateButton.setToolTipText(Bundle.getMessage("ToolTipPickFromTable"));
                _reporter = new ReporterIcon(_editor);
            }
        }
        initIconFamiliesPanel();
        validate();
    }

    @Override
    protected void showIcons() {
    }

    @Override
    protected void setEditor(Editor ed) {
        _family = null;
        super.setEditor(ed);
        if (_initialized) {
            remove(_iconFamilyPanel);
            initIconFamiliesPanel();
            add(_iconFamilyPanel, 1);
            validate();
        }
    }

    protected IconDragJComponent getDragger(DataFlavor flavor) {
        return new IconDragJComponent(flavor, _reporter);
    }

    protected class IconDragJComponent extends DragJComponent {

        public IconDragJComponent(DataFlavor flavor, JComponent comp) {
            super(flavor, comp);
        }
        
        @Override
        protected boolean okToDrag() {
            NamedBean bean = getDeviceNamedBean();
            if (bean == null) {
                JOptionPane.showMessageDialog(this, Bundle.getMessage("noRowSelected"),
                        Bundle.getMessage("WarningTitle"), JOptionPane.WARNING_MESSAGE);
                return false;
            }
            return true;
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor)) {
                return null;
            }
            NamedBean bean = getDeviceNamedBean();
            if (bean == null) {
                return null;
            }

            if (flavor.isMimeTypeEqual(Editor.POSITIONABLE_FLAVOR)) {
                ReporterIcon r = new ReporterIcon(_editor);
                r.setReporter(bean.getDisplayName());
                r.setLevel(Editor.REPORTERS);
                return r;                
            } else if (DataFlavor.stringFlavor.equals(flavor)) {
                StringBuilder sb = new StringBuilder(_itemType);
                sb.append(" icon for \"");
                sb.append(bean.getDisplayName());
                sb.append("\"");
                return  sb.toString();
            }
            return null;
        }
    }

    private final static Logger log = LoggerFactory.getLogger(ReporterItemPanel.class);

}
