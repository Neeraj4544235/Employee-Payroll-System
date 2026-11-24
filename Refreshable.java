package payroll;

/** Every screen (panel) implements this so it can reload its data when opened. */
public interface Refreshable {
    void refresh();
}
