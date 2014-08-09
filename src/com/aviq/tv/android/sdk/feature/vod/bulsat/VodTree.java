package com.aviq.tv.android.sdk.feature.vod.bulsat;

import java.util.ArrayList;
import java.util.List;

public class VodTree<T>
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

	public static class Node<T>
	{
		private T _data;
		private Node<T> _parent;
		private List<Node<T>> _children;

		public Node<T> add(T data)
		{
			Node<T> node = new Node<T>();
			node._data = data;
			node._children = new ArrayList<Node<T>>();
			node._parent = this;

			_children.add(node);
			return node;
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
	}
}
