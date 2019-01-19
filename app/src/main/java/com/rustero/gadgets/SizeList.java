package com.rustero.gadgets;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;


public class SizeList {
    private ArrayList<Size2D> mList = new ArrayList<Size2D>();


	public SizeList() {	}



    public SizeList(android.util.Size[] aSizeArray) {
        for (int i=0; i<aSizeArray.length; i++) {
            android.util.Size size = aSizeArray[i];
            addSize(new Size2D(size.getWidth(), size.getHeight()));
        }
    }


    public void clear() {
        mList.clear();
    }


    public int length() {
        return mList.size();
    }


    public void sort() {
        Collections.sort(mList);
    }



    public String[] getTextArray() {
        String[] result = new String[mList.size()];
		if (mList.size() == 0) return result;
		for (int i=0; i<mList.size(); i++) {
			result[i] = mList.get(i).toText();
		}
        return result;
    }



    public Size2D get(int aIndex) {
        Size2D result = null;
        if (aIndex < mList.size())
            result = mList.get(aIndex);
        return result;
    }



    public boolean hasSize(Size2D aSize) {
        boolean result = false;
        for (Size2D size : mList) {
            if (size.x == aSize.x && size.y == aSize.y) {
                result = true;
                break;
            }
        }
        return result;
    }



    public void addSize(Size2D aSize) {
        mList.add(aSize);
    }



	public int findSize(Size2D aSize) {
		int result = -1;
		for (int i=0; i<mList.size(); i++) {
			Size2D size = mList.get(i);
			if ((size.x == aSize.x) && (size.y == aSize.y)) {
				result = i;
				break;
			}
		}
		return result;
	}



	public int findResoText(String aResoText) {
		int result = -1;
		Size2D size = Size2D.parseText(aResoText);
		result = findSize(size);
		return result;
	}



	public void addUnique(int aWidth, int aHeight) {
        Size2D size = new Size2D(aWidth, aHeight);
        boolean hasit = hasSize(size);
        if (hasit) return;
        addSize(size);
    }



    public int findByHeight(int aHeight) {
        int result = -1;
        for (int i=0; i<mList.size(); i++) {
            Size2D size = mList.get(i);
            if (size.y >= aHeight) {
                result = i;
                break;
            }
        }
        return result;
    }




    public Size2D getAboveHeight(int aHeight) {
        Size2D result = null;
        for (int i=0; i<mList.size(); i++) {
            Size2D size = mList.get(i);
            if (size.y >= aHeight) {
                result = size;
                break;
            }
        }
        if (null == result) {
            if (mList.size() > 0)
                result = mList.get(mList.size()-1);
        }
        return result;
    }



    public Size2D getBelowHeight(int aHeight) {
        Size2D result = null;
        for (int i=mList.size()-1; i>=0; i--) {
            Size2D size = mList.get(i);
            if (size.y <= aHeight) {
                result = size;
                break;
            }
        }
        if (null == result) {
            if (mList.size() > 0)
                result = mList.get(0);
        }
        return result;
    }



    public Size2D getLargest() {
        if (mList.size() > 0)
            return mList.get(mList.size()-1);
        else
            return null;
    }



    public void deleteHigherThan(int aHeight) {
		Iterator<Size2D> it = mList.iterator();
		while (it.hasNext()) {
			Size2D size = it.next();
			if (size.y > aHeight) {
				it.remove();
			}
		}
    }

}
