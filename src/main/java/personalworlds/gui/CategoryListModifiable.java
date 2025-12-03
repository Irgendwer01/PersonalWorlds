package personalworlds.gui;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.CategoryList;

public class CategoryListModifiable extends CategoryList {

    @Override
    public CategoryListModifiable getThis() {
        return (CategoryListModifiable) super.getThis();
    }

    // Taken from IParentWidget
    public CategoryListModifiable child(IWidget child) {
        if (!this.addChild(child, -1)) {
            throw new IllegalStateException("Failed to add child");
        } else {
            return this.getThis();
        }
    }

    @Override
    public void onChildAdd(IWidget child) {
        super.onChildAdd(child);
        if (this.isValid()) {
            this.scheduleResize();
        }
    }

    @Override
    protected void onChildRemove(IWidget child) {
        super.onChildRemove(child);
        if (this.isValid()) {
            this.scheduleResize();
        }
    }

    @Override
    public boolean removeAll() {
        return super.removeAll();
    }
}
