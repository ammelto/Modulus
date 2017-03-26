package io.seamoss.modulus.base;

import android.support.annotation.IntRange;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Modified by Alexander Melton
 *
 * Borrowed from Marcus Gabilheri. Base adapter was too good to pass up!
 *
 * Created by <a href="mailto:marcus@gabilheri.com">Marcus Gabilheri</a>
 *
 * @author Marcus Gabilheri
 * @version 1.0
 * @since 12/5/16.
 */
public abstract class BaseRecyclerAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {

    protected List<T> elements;

    private int viewtype;

    public BaseRecyclerAdapter() {
        elements = new ArrayList<>();
    }

    public BaseRecyclerAdapter(@NonNull List<T> elements) {
        elements = elements;
    }

    /**
     * Adds an item to the list and calls the notifyItemInserted
     * @param element
     *      The element to be added
     */
    public void add(T element) {
        elements.add(element);
        notifyItemInserted(getItemCount() - 1);
    }

    /**
     * Adds all the elements to the list and notifies the data set changed
     * @param elements
     *      The element to be added
     */
    public void addAll(@NonNull List<T> elements) {
        this.elements.addAll(elements);
        notifyDataSetChanged();
    }

    /**
     * Gets item at a specific position
     * @param position
     *      The index of the item
     * @return
     *      The element at the index
     */
    public T get(int position) {
        return this.elements.get(position);
    }

    /**
     * Removes an element from the list
     * @param element
     *      the element to be removed
     */
    public boolean remove(T element) {
        int index = elements.indexOf(element);
        if (index != -1) {
            this.elements.remove(element);
            notifyItemRemoved(index);
            return true;
        }
        return false;
    }

    /**
     * removes element at a specific index
     * @param position
     *      The position of the element
     */
    public boolean removeAt(@IntRange(from = 0) int position) {
        if (position > elements.size() - 1) {
            return false;
        }
        this.elements.remove(position);
        notifyItemRemoved(position);
        return true;
    }

    /**
     * Swaps the data of the adapter
     * @param elements
     *      The new elements of the list
     */
    public void swapData(@NonNull List<T> elements) {
        this.elements.clear();
        addAll(elements);
    }

    /**
     * Clears the adapter
     */
    public void clear() {
        this.elements.clear();
        notifyDataSetChanged();
    }

    /**
     * Gets the elements of the adapter
     * @return
     *      List of the current elements in the adapter
     */
    public List<T> getElements() {
        return this.elements;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        this.viewtype = viewType;
        View view = LayoutInflater.from(parent.getContext()).inflate(getLayoutRes(), parent, false);
        return inflateViewHolder(view);
    }

    public int getViewtype() {
        return viewtype;
    }

    @Override
    public int getItemCount() {
        return elements.size();
    }

    /**
     * The layout resource to be used by the ViewHolder of this Adapter
     * @return
     *      The integer resource layout used by the ViewHolder
     */
    protected abstract @LayoutRes
    int getLayoutRes();

    /**
     * Inflates the ViewHolder of the
     * @param v
     *      The view for the ViewHolder
     * @return
     *      The ViewHolder with the inflated view
     */
    protected abstract VH inflateViewHolder(View v);
}
