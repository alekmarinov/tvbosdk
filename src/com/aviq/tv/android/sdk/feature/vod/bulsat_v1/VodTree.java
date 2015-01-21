package com.aviq.tv.android.sdk.feature.vod.bulsat_v1;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class VodTree<T extends Parcelable> implements Parcelable
{
	private Node<T> _root;

	public VodTree(T rootData)
	{
		_root = new Node<T>();
		_root._data = rootData;
		_root._children = new ArrayList<Node<T>>();
	}

	public Node<T> getRoot()
	{
		return _root;
	}

	public ArrayList<Node<T>> findPath(T data)
	{
		ArrayList<Node<T>> path = new ArrayList<Node<T>>();
		findNode(getRoot(), data, path);
		return path;
	}
	
	private Node<T> findNode(Node<T> nodeToExplore, T data, ArrayList<Node<T>> path)
	{
		if (nodeToExplore == null)
			return null;

		if (nodeToExplore.getData().equals(data))
		{
			while (!nodeToExplore.equals(getRoot()))
			{
				path.add(0, nodeToExplore);
				nodeToExplore = nodeToExplore.getParent();
			}
			return nodeToExplore;
		}

		for (VodTree.Node<T> child : nodeToExplore.getChildren())
		{
			Node<T> foundNode = findNode(child, data, path);
			if (foundNode != null)
				return foundNode;
		}

		return null;
	}

	public static class Node<T extends Parcelable> implements Parcelable
	{
		private T _data;
		private Node<T> _parent;
		private List<Node<T>> _children;

		public Node()
		{}
		
		public Node<T> add(T data)
		{
			Node<T> node = new Node<T>();
			node._data = data;
			node._children = new ArrayList<Node<T>>();
			node._parent = this;

			_children.add(node);
			return node;
		}

		public void remove(T data)
		{
			for (Iterator<VodTree.Node<T>> iter = _children.iterator(); iter.hasNext(); )
			{
				VodTree.Node<T> child = iter.next();
				if (child.getData().equals(data))
				{
					iter.remove();
					break;
				}
			}
		}
		
		public List<Node<T>> getChildren()
		{
			return _children;
		}

		public Node<T> getChildAt(int index)
		{
			return _children.size() > index ? _children.get(index) : null;
		}

		public boolean hasChildren()
		{
			return _children.size() > 0;
		}

		public Node<T> getParent()
		{
			return _parent;
		}

		public boolean hasParent()
		{
			return _parent != null;
		}

		public T getData()
		{
			return _data;
		}
		
		// Parcelable contract
		
		public Node(Parcel in)
		{
			_data = in.readParcelable(_data.getClass().getClassLoader());
			_parent = in.readParcelable(_parent.getClass().getClassLoader());
			_children = new ArrayList<Node<T>>();
			in.readList(_children, null);
		}
		
		@Override
	    public int describeContents()
		{
	        return 0;
	    }
		
		@Override
	    public void writeToParcel(Parcel dest, int flags) 
	    {
			dest.writeParcelable((Parcelable) _data, flags);
			dest.writeParcelable(_parent, flags);
			dest.writeList(_children);
	    }
	    
	    public static final Parcelable.Creator<Node> CREATOR = new Parcelable.Creator<Node>() 
	    {
	        public Node createFromParcel(Parcel in) 
	        {
	            return new Node(in); 
	        }

	        public Node[] newArray(int size) 
	        {
	            return new Node[size];
	        }
	    };
	}
	
	// Parcelable contract
	
	public VodTree(Parcel in)
	{
		_root = in.readParcelable(_root.getClass().getClassLoader());
	}
	
	@Override
    public int describeContents()
	{
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) 
    {
    	dest.writeParcelable(_root, flags);
    }
    
    public static final Parcelable.Creator<VodTree> CREATOR = new Parcelable.Creator<VodTree>() 
    {
        public VodTree createFromParcel(Parcel in) 
        {
            return new VodTree(in); 
        }

        public VodTree[] newArray(int size) 
        {
            return new VodTree[size];
        }
    };
}
