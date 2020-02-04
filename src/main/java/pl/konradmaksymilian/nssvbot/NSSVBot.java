package pl.konradmaksymilian.nssvbot;

import java.io.IOException;

import pl.konradmaksymilian.nssvbot.management.AppManager;
import pl.konradmaksymilian.nssvbot.management.ConsoleManager;
import pl.konradmaksymilian.nssvbot.session.SessionFactory;
import pl.konradmaksymilian.nssvbot.session.SessionsManager;

public class NSSVBot {
	
	public static void main(String[] args) throws InterruptedException, IOException {
		new AppManager(new ConsoleManager(), new SessionsManager(new SessionFactory()));
	}
}
