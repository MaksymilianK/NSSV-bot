package pl.konradmaksymilian.nssvbot.management;

import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.function.Consumer;

import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.argThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import pl.konradmaksymilian.nssvbot.management.command.AttachCommand;
import pl.konradmaksymilian.nssvbot.management.command.Command;
import pl.konradmaksymilian.nssvbot.management.command.DetachCommand;
import pl.konradmaksymilian.nssvbot.management.command.JoinCommand;
import pl.konradmaksymilian.nssvbot.management.command.detached.AdCommandDetached;
import pl.konradmaksymilian.nssvbot.session.SessionsManager;

public class AppManagerTest {
    
    private AppManager appManager;
    
    private Consumer<String> onInput;
    
    @Mock
    private ConsoleManager console;
    
    @Mock
    private SessionsManager sessionsManager;
    
    @BeforeEach
    private void setUp() {
        MockitoAnnotations.initMocks(this);
        doAnswer(invocation -> onInput = invocation.getArgument(0)).when(console).onInput(any());
        appManager = new AppManager(console, sessionsManager);
    }
    
    @Test
    public void initializingSessionsManager() {
        verify(sessionsManager).onMessage(any());
        verify(sessionsManager).setPlayers(any());
    }
    
    @Test
    public void initializingConsoleManager() throws InterruptedException, IOException {
        verify(console).onInput(any());
    }
    
    @Test
    public void joinOnInputIfSessionsManagerWaitingForPassword() {
        when(sessionsManager.waitsForPassword()).thenReturn(true);
        onInput.accept("password");
        
        verify(sessionsManager).join("password");
    }
    
    @Test
    public void processJoinCommandIfNoActiveSession() {
        when(sessionsManager.waitsForPassword()).thenReturn(false);
        when(sessionsManager.isAnyActive()).thenReturn(false);
        
        onInput.accept("!join player1");
        
        verify(sessionsManager).join(argThat((JoinCommand command) -> command.getNickOrAlias().equals("player1")));
    }
    
    @Test
    public void askForPasswordIfJoinReturnsFalse() {
        when(sessionsManager.waitsForPassword()).thenReturn(false);
        when(sessionsManager.isAnyActive()).thenReturn(false);
        when(sessionsManager.join(any(JoinCommand.class))).thenReturn(false);
        
        onInput.accept("!join player1");
        
        verify(console).write("Password: ", false);
    }
    
    @Test
    public void attachingToSession() {        
        onInput.accept("!attach player1");
        
        verify(sessionsManager).attach(argThat(command -> command.getNickOrAlias().equals("player1")));
    }
    
    @Test
    public void detachingFromSession() {        
        onInput.accept("!detach");
        
        verify(sessionsManager).detach();
    }
    
    @Test
    public void settingAdActive() {
        when(sessionsManager.isAnyActive()).thenReturn(true);
        onInput.accept("!ad 65 messagePart1 messagePart2 messagePart3");
        
        verify(sessionsManager).setAdActive(argThat(command -> command.getDuration() == 65 
                && command.getText().equals("messagePart1 messagePart2 messagePart3")));
    }
    
    @Test
    public void settingAdDetached() {
        when(sessionsManager.isAnyActive()).thenReturn(false);
        onInput.accept("!ad player1 65 messagePart1 messagePart2 messagePart3");
        
        verify(sessionsManager).setAdDetached(argThat(command -> command.getNickOrAlias().equals("player1")
                && command.getDuration() == 65 && command.getText().equals("messagePart1 messagePart2 messagePart3")));
    }
    
    @Test
    public void leavingActive() {
        when(sessionsManager.isAnyActive()).thenReturn(true);
        
        onInput.accept("!leave");
        
        verify(sessionsManager).leaveActive();
    }
    
    @Test
    public void leavingDetached() {
        when(sessionsManager.isAnyActive()).thenReturn(false);
        
        onInput.accept("!leave player1");
        
        verify(sessionsManager).leaveDetached(argThat(command -> command.getNickOrAlias().equals("player1")));
    }
    
    @Test
    public void sendMessageIfSessionActive() {
        when(sessionsManager.isAnyActive()).thenReturn(true);
        
        onInput.accept("message");
        
        verify(sessionsManager).sendMessage("message");
    }
    
    @Test
    public void writeErrorIfCommandDoNotStartWithExclamationMark() {
        onInput.accept("join player1");
        
        verify(console).writeLine("It is not a valid input!", true);
    }
    
    @Test
    public void writeErrorIfJoinCommandTooLong() {
        onInput.accept("!join player1 player2");
        
        verify(console).writeLine("Command 'join [nickOrAlias]' is too long", true);
    }
    
    @Test
    public void writeErrorIfAttachCommandTooShort() {
        onInput.accept("!attach");
        
        verify(console).writeLine("Command 'attach [nickOrAlias]' is too short", true);
    }
    
    @Test
    public void writeErrorIfAttachCommandTooLong() {
        onInput.accept("!attach player1 player2");
        
        verify(console).writeLine("Command 'attach [nickOrAlias]' is too long", true);
    }
    
    @Test
    public void writeErrorIfDetachCommandTooLong() {
        onInput.accept("!detach player1");
        
        verify(console).writeLine("Command 'detach (no params)' is too long", true);
    }
    
    @Test
    public void writeErrorIfAdCommandActiveTooShort() {
        when(sessionsManager.isAnyActive()).thenReturn(true);
        onInput.accept("!ad 65");
        
        verify(console).writeLine("Command 'ad [duration] [text]' is too short", true);
    }
    
    @Test
    public void writeErrorIfAdCommandActiveDoesNotHaveDuration() {
        when(sessionsManager.isAnyActive()).thenReturn(true);
        onInput.accept("!ad message message message");
        
        verify(console).writeLine("Command 'ad [duration (in seconds)] [text]' - parameter 'duration' should"
                + " be a number!", true);
    }
    
    @Test
    public void writeErrorIfAdCommandDetachedTooShort() {
        when(sessionsManager.isAnyActive()).thenReturn(false);
        onInput.accept("!ad 65 player1");
        
        verify(console).writeLine("Command 'ad [nickOrAlias] [duration (in seconds)] [text]' is too short", true);
    }
    
    @Test
    public void writeErrorIfAdCommandDetachedDoesNotHaveDuration() {
        when(sessionsManager.isAnyActive()).thenReturn(false);
        onInput.accept("!ad player1 message message message");
        
        verify(console).writeLine("Command 'ad [nickOrAlias] [duration (in seconds)] [text]' - parameter "
                + "'duration' should be a number!", true);
    }
    
    @Test
    public void writeErrorIfLeaveCommandActiveTooLong() {
        when(sessionsManager.isAnyActive()).thenReturn(true);
        onInput.accept("!leave player1");
        
        verify(console).writeLine("Command 'leave (no params)' is too long", true);
    }
    
    @Test
    public void writeErrorIfLeaveCommandDetachedTooLong() {
        when(sessionsManager.isAnyActive()).thenReturn(false);
        onInput.accept("!leave player1 player2");
        
        verify(console).writeLine("Command 'leave [nickOrAlias]' is too long", true);
    }
}
