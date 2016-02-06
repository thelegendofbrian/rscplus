/**
 *	rscplus
 *
 *	This file is part of rscplus.
 *
 *	rscplus is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	rscplus is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with rscplus.  If not, see <http://www.gnu.org/licenses/>.
 *
 *	Authors: see <https://github.com/OrN/rscplus>
 */

package Client;

import Game.Game;
import java.applet.Applet;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class Launcher extends JFrame implements Runnable
{
	public void init()
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().setBackground(Color.BLACK);

		// Set window icon
		URL iconURL = Settings.getResource("/assets/icon.png");
		if(iconURL != null)
		{
			ImageIcon icon = new ImageIcon(iconURL);
			setIconImage(icon.getImage());
		}

		// Set size
		getContentPane().setPreferredSize(new Dimension(280, 98));
		getContentPane().setLayout(null);
		setResizable(false);
		setTitle("rscplus Launcher");
		pack();
		pack();
		setLocationRelativeTo(null);

		// Add components
		JLabel label = new JLabel("World: ");
		label.setForeground(Color.GRAY.brighter());
		label.setSize(64, 12);
		label.setLocation(42, 16);
		getContentPane().add(label);

		m_worldSelector = new JComboBox();
		for(int i = 0; i < Settings.WORLD_LIST.length; i++)
			m_worldSelector.addItem(Settings.WORLD_LIST[i]);
		m_worldSelector.setSelectedIndex(Settings.WORLD - 1);
		m_worldSelector.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				Settings.WORLD = m_worldSelector.getSelectedIndex() + 1;
			}
		});
		m_worldSelector.setSize(150, 20);
		m_worldSelector.setLocation(label.getX() + label.getWidth(), label.getY() - 4);
		getContentPane().add(m_worldSelector);

		m_progressBar = new JProgressBar();
		m_progressBar.setStringPainted(true);
		m_progressBar.setBorderPainted(true);
		m_progressBar.setForeground(Color.GRAY.brighter());
		m_progressBar.setBackground(Color.BLACK);
		m_progressBar.setString("Waiting for game to be launched...");
		m_progressBar.setSize(getContentPane().getWidth() - 16, 20);
		m_progressBar.setLocation(8, m_worldSelector.getY() + 30);
		getContentPane().add(m_progressBar);

		//pack();
		m_launchButton = new JButton("Launch");
		m_launchButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_worldSelector.setEnabled(false);
				m_launchButton.setEnabled(false);
				new Thread(Launcher.instance).start();
			}
		});
		m_launchButton.setSize(100, 20);
		m_launchButton.setLocation((getContentPane().getWidth() / 2) - (m_launchButton.getWidth() / 2),
					   getContentPane().getHeight() - m_launchButton.getHeight() - 8);
		getContentPane().add(m_launchButton);

		setVisible(true);
	}

	public void run()
	{
		Settings.Save();

		setStatus("Loading JConfig...");
		JConfig config = Game.getInstance().getJConfig();
		if(!config.fetch(Util.MakeWorldURL(Settings.WORLD)))
		{
			error("Unable to fetch JConfig");
			return;
		}

		if(!config.isSupported())
		{
			error("JConfig outdated");
			return;
		}

		m_classLoader = new JClassLoader();
		if(!m_classLoader.fetch(this, config.getJarURL()))
		{
			error("Unable to fetch Jar");
		}

		setStatus("Launching game...");
		Game game = Game.getInstance();
		try
		{
			Class<?> client = m_classLoader.loadClass(config.getJarClass());
			game.setApplet((Applet)client.newInstance());
		}
		catch(Exception e)
		{
			error("Unable to launch game");
			e.printStackTrace();
			return;
		}
		setVisible(false);
		dispose();
		game.start();
	}

	public void error(String text)
	{
		setStatus("Error: " + text);
		setProgress(0, 0);
		m_worldSelector.setEnabled(true);
		m_launchButton.setEnabled(true);
	}

	public void setStatus(final String text)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_progressBar.setString(text);
			}
		});
	}

	public void setProgress(final int value, final int total)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if(total == 0)
				{
					m_progressBar.setValue(0);
					return;
				}

				m_progressBar.setValue(value * 100 / total);
			}
		});
	}

	public JClassLoader getClassLoader()
	{
		return m_classLoader;
	}

	public static void main(String args[])
	{
		Settings.Load();
		Launcher.getInstance().init();
	}

	public static Launcher getInstance()
	{
		return (instance == null)?(instance = new Launcher()):instance;
	}

	private JComboBox m_worldSelector;
	private JProgressBar m_progressBar;
	private JButton m_launchButton;
	private JClassLoader m_classLoader;

	// Singleton
	private static Launcher instance;
}
