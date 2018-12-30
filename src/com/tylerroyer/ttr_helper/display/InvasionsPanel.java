package com.tylerroyer.ttr_helper.display;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.IllegalComponentStateException;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import com.tylerroyer.ttr_helper.cogs.Cog;
import com.tylerroyer.ttr_helper.globals.GlobalFonts;
import com.tylerroyer.ttr_helper.globals.GlobalHandles;
import com.tylerroyer.ttr_helper.invasions.Invasion;
import com.tylerroyer.ttr_helper.invasions.InvasionAPIHandler;

public class InvasionsPanel extends JPanel {
	private JPanel invasionPanelListContainer;

	private long timeOfLastInvasionsCheck;

	public InvasionsPanel() {
		invasionPanelListContainer = new JPanel();
		buildInvasionPanelListContainer();
		timeOfLastInvasionsCheck = System.currentTimeMillis();

		JScrollPane scrollPane = new JScrollPane(invasionPanelListContainer);
		scrollPane.getVerticalScrollBar().setUnitIncrement(12);

		this.setLayout(new BorderLayout());
		this.add(scrollPane, BorderLayout.CENTER);

		JLabel header = new JLabel("Active invasions (updates automatically)");
		header.setHorizontalAlignment(SwingConstants.CENTER);
		header.setFont(GlobalFonts.standardFont.deriveFont(45f));
		this.add(header, BorderLayout.PAGE_START);
	}

	public void doUpdate(boolean isSelected) {
		if (isSelected) {
			for (Component c : invasionPanelListContainer.getComponents()) {
				if (c instanceof InvasionPanel)
					((InvasionPanel) c).update();
			}

			// Update scroller size
			invasionPanelListContainer.setPreferredSize(new Dimension(225,
					invasionPanelListContainer.getComponent(invasionPanelListContainer.getComponentCount() - 1).getY()
							+ 207));

			// Requary api
			if (System.currentTimeMillis() - timeOfLastInvasionsCheck > 10 * 1000) {
				checkForAPIUpdates();
				timeOfLastInvasionsCheck = System.currentTimeMillis();
			}
		}
	}

	public void checkForAPIUpdates() {
		invasionPanelListContainer.removeAll();
		Graphics2D panelGraphics = (Graphics2D) invasionPanelListContainer.getGraphics();
		panelGraphics.clearRect(0, 0, GlobalHandles.windowHandle.getWidth() - 25,
				GlobalHandles.windowHandle.getHeight());
		panelGraphics.setFont(GlobalFonts.standardFont.deriveFont(22.5f));
		panelGraphics.drawString("Retrieving updated invasion data...", 5, 40);
		this.revalidate();
		buildInvasionPanelListContainer();
		this.revalidate();
	}

	private void buildInvasionPanelListContainer() {
		InvasionAPIHandler apiHandler = new InvasionAPIHandler();

		// Alert user if connection timed out
		if (apiHandler.getInvasionInfo().isError()) {
			Graphics2D panelGraphics = (Graphics2D) invasionPanelListContainer.getGraphics();
			if (panelGraphics != null)
				panelGraphics.clearRect(0, 0, GlobalHandles.windowHandle.getWidth() - 25,
						GlobalHandles.windowHandle.getHeight());
			JLabel errorLabel = new JLabel("Internet connection timed out!  Trying again on next update...");
			errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
			errorLabel.setFont(GlobalFonts.standardFont.deriveFont(22.5f));
			errorLabel.setForeground(Color.RED);
			invasionPanelListContainer.add(errorLabel, BorderLayout.CENTER);
			return;
		}

		// Alert user if there are no active invasions
		if (apiHandler.getInvasionInfo().getInvasionList().isEmpty()) {
			Graphics2D panelGraphics = (Graphics2D) invasionPanelListContainer.getGraphics();
			if (panelGraphics != null)
				panelGraphics.clearRect(0, 0, GlobalHandles.windowHandle.getWidth() - 25,
						GlobalHandles.windowHandle.getHeight());
			JLabel noInvasionsLabel = new JLabel("There are currently no active invasions!");
			noInvasionsLabel.setHorizontalAlignment(SwingConstants.CENTER);
			noInvasionsLabel.setFont(GlobalFonts.standardFont.deriveFont(22.5f));
			invasionPanelListContainer.add(noInvasionsLabel, BorderLayout.CENTER);
			return;
		}

		for (Invasion i : apiHandler.getInvasionInfo().getInvasionList()) {
			System.out.println("There is a " + i.getCog().getName() + " invasion in " + i.getDistrict()
					+ ".  Current progress: " + i.getCurrentProgress() + "/" + i.getMaxProgress());

			invasionPanelListContainer.add(new InvasionPanel(i));
		}
		System.out.println();

		invasionPanelListContainer
				.setPreferredSize(new Dimension(225, apiHandler.getInvasionInfo().getInvasionList().size() * 207));
	}

	private class InvasionPanel extends JPanel {
		private Invasion invasion;

		public InvasionPanel(Invasion i) {
			this.invasion = i;

			BufferedImage panelImage = new BufferedImage(893, 192, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = panelImage.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);

			try {
				// Background
				BufferedImage background = ImageIO.read(
						this.getClass().getResourceAsStream("/resources/graphical/Invasion Panel Background.png"));
				g.drawImage(background, 0, 0, this);

				// Cog picture
				g.drawImage(Cog.findCogByName(i.getCog().getName()).getImage(), 42, 32, this);
				g.setColor(Color.BLACK);
				g.setStroke(new BasicStroke(5));
				g.drawRect(39, 29, 133, 133);

				// Cog name
				g.setFont(GlobalFonts.standardFont);
				g.drawString("Cog Type:", 200, 55);
				g.setFont(GlobalFonts.cogFont);
				g.drawString(i.getCog().getName(), 350, 55);

				// District name
				g.setFont(GlobalFonts.standardFont);
				g.drawString("District:", 200, 105);
				g.drawString(i.getDistrict(), 350, 105);

				// Progress
				g.drawString("Progress:", 200, 155);
				g.setColor(new Color(0, 128, 0));
				g.fillRect(353, 133, 246 * i.getCurrentProgress() / i.getMaxProgress(), 27);
				g.setColor(Color.BLACK);
				g.drawRect(350, 130, 250, 32);
				g.drawString("(" + i.getCurrentProgress() + " / " + i.getMaxProgress() + ")", 630, 155);

			} catch (Exception e) {
				e.printStackTrace();
			}

			add(new JLabel(new ImageIcon(panelImage)));
		}

		public void update() {
			// Get mouse info
			Point mLoc = MouseInfo.getPointerInfo().getLocation();
			try {
				mLoc = new Point(mLoc.x - this.getLocationOnScreen().x, mLoc.y - this.getLocationOnScreen().y);
			} catch (IllegalComponentStateException e) {
				// Do nothing. This is a result of the tab switching before the component shows
				// up. It's okay.
			}

			// Show hover info
			if (mLoc.x < 182 && mLoc.x > 43 && mLoc.y > 32 && mLoc.y < 151) {
				// TODO Not double-buffering before drawing hover image
				// It'd be nice if I could get this to work, but it doesn't seem like I can for
				// right now. Maybe just inflate the image in another window when the icon is
				// pressed?
				// this.getGraphics().drawImage(invasion.getCog().getInfoImage(), mLoc.x,
				// mLoc.y, this);
				// this.repaint();
			}
		}

		public Invasion getInvasion() {
			return invasion;
		}
	}
}
