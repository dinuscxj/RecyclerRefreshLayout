package app.dinus.com.refresh;

/**
 * {@link RecyclerRefreshLayout#mRefreshView} all the custom drop-down refresh view need to implements the interface
 */
public interface IRefreshStatus {
    //reset the state of the refreshView
    void reset();

    //refreshView is refreshing
    void refreshing();

    //refreshView is dropped down to the refresh point
    void pullToRefresh();

    //refreshView is released into the refresh point
    void releaseToRefresh();

    //the drop-down progress of the refreshView and the pullProgress may be more than 1.0f
    void pullProgress(float pullDistance, float pullProgress);
}
