package tools.ds.modkit;

/** Implement and register via ServiceLoader to add overrides. */
public interface PatchProvider {
    void register(OverrideRegistry reg);
}
