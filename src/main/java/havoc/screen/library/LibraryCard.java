package havoc.screen.library;

import net.minecraft.resources.Identifier;

/** A view of a vanilla entry; no world or server persistence is owned by the UI. */
public interface LibraryCard {
   String id();
   String title();
   String description();
   String detail();
   Identifier image();
   void select();
}
