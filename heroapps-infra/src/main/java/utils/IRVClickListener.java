package utils;

import android.view.View;

/**
 * Created by Refael Ozeri on 08/05/2017.
 */

public interface IRVClickListener {
  void onClick(View view, int position);
  void onLongClick(View view, int position);
}