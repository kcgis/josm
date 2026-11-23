// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bugreport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.PlatformManager;

/**
 * This is a panel that displays the current JME version and the ability to update JME.
 * @author Michael Zangl
 * @since 10649
 */
public class JosmUpdatePanel extends JPanel {
    private final JMultilineLabel testedVersionField;
    private final int josmVersion;

    /**
     * Create a new {@link JosmUpdatePanel}
     */
    public JosmUpdatePanel() {
        super(new GridBagLayout());
        josmVersion = Version.getInstance().getVersion();

        add(new JMultilineLabel(tr("Your current version of JME is {0}", Integer.toString(josmVersion))), GBC.eol().fill(GBC.HORIZONTAL));
        testedVersionField = new JMultilineLabel(tr("JME is searching for updates..."));
        add(testedVersionField, GBC.eol().fill(GBC.HORIZONTAL));

        checkCurrentVersion();
    }

    private void checkCurrentVersion() {
        new Thread(this::readCurrentVersion, "JME version checker").start();
    }

    private void readCurrentVersion() {
        try {
            int testedVersion = getTestedVersion();

            if (testedVersion < 0) {
                SwingUtilities.invokeLater(this::displayError);
            } else if (josmVersion < testedVersion) {
                SwingUtilities.invokeLater(() -> displayOutOfDate(testedVersion));
            } else {
                SwingUtilities.invokeLater(this::displayUpToDate);
            }
        } catch (RuntimeException e) {
            Logging.error(e);
        }
    }

    private static int getTestedVersion() {
        try (CachedFile testedVersion = new CachedFile(Config.getUrls().getJOSMWebsite() + "/jme-version")) {
            testedVersion.setMaxAge(60L * 15); // 15 Minutes
            String testedString = new String(testedVersion.getByteContent(), StandardCharsets.ISO_8859_1);
            return Integer.parseInt(testedString.trim());
        } catch (NumberFormatException | IOException e) {
            Logging.log(Logging.LEVEL_WARN, "Unable to detect current tested version of JME:", e);
            return -1;
        }
    }

    /**
     * Display that there was an error while checking the current version.
     */
    private void displayError() {
        testedVersionField.setText(tr("An error occurred while checking if your JME instance is up to date."));
        showUpdateButton();
    }

    private void displayUpToDate() {
        testedVersionField.setText(tr("JME is up to date."));
    }

    private void displayOutOfDate(int testedVersion) {
        testedVersionField
                .setText(tr("JME is out of date. The current version is {0}. Try updating JME.", Integer.toString(testedVersion)));
        showUpdateButton();
    }

    private void showUpdateButton() {
        add(new JMultilineLabel(tr("Before you file a bug report make sure you have updated to the latest version of JME here:")), GBC.eol());
        add(new UrlLabel(Config.getUrls().getJOSMWebsite(), 2), GBC.eop().insets(8, 0, 0, 0));
        JButton updateButton = new JButton(tr("Update JME"), ImageProvider.getIfAvailable("download"));
        updateButton.addActionListener(e -> openJosmUpdateSite());
        add(updateButton, GBC.eol().anchor(GBC.EAST));
    }

    private static void openJosmUpdateSite() {
        try {
            PlatformManager.getPlatform().openUrl(Config.getUrls().getJOSMWebsite());
        } catch (IOException ex) {
            Logging.log(Logging.LEVEL_WARN, "Unable to access JME website:", ex);
        }
    }
}
