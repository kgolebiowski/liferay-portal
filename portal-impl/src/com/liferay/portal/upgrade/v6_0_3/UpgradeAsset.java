/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.upgrade.v6_0_3;

import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.LoggingTimer;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Julio Camarero
 * @author Amos Fong
 */
public class UpgradeAsset extends UpgradeProcess {

	protected void createIndex() {
		try (LoggingTimer loggingTimer = new LoggingTimer()) {
			runSQL(
				"create unique index IX_1E9D371D on AssetEntry (classNameId, " +
					"classPK)");
		}
		catch (Exception e) {
		}
	}

	@Override
	protected void doUpgrade() throws Exception {
		createIndex();

		updateAssetEntries();
	}

	protected String getUuid(
			String tableName, String columnName1, String columnName2,
			long classPK)
		throws Exception {

		String uuid = StringPool.BLANK;

		try (PreparedStatement ps = connection.prepareStatement(
				"select uuid_ from " + tableName + " where " + columnName1 +
					" = ? or " + columnName2 + " = ?")) {

			ps.setLong(1, classPK);
			ps.setLong(2, classPK);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					uuid = rs.getString("uuid_");
				}
			}
		}

		return uuid;
	}

	protected void updateAssetEntries() throws Exception {
		try (LoggingTimer loggingTimer = new LoggingTimer()) {
			updateAssetEntry(
				"com.liferay.portal.model.User", "User_", "userId");
			updateAssetEntry(
				"com.liferay.portlet.blogs.model.BlogsEntry", "BlogsEntry",
				"entryId");
			updateAssetEntry(
				"com.liferay.portlet.bookmarks.model.BookmarksEntry",
				"BookmarksEntry", "entryId");
			updateAssetEntry(
				"com.liferay.portlet.calendar.model.CalEvent", "CalEvent",
				"eventId");
			updateAssetEntry(
				"com.liferay.portlet.documentlibrary.model.DLFileEntry",
				"DLFileEntry", "fileEntryId");
			updateAssetEntry(
				"com.liferay.portlet.documentlibrary.model.DLFileShortcut",
				"DLFileShortcut", "fileShortcutId");
			updateAssetEntry(
				"com.liferay.portlet.imagegallery.model.IGImage", "IGImage",
				"imageId");
			updateAssetEntry(
				"com.liferay.portlet.journal.model.JournalArticle",
				"JournalArticle", "resourcePrimKey", "id_");
			updateAssetEntry(
				"com.liferay.portlet.messageboards.model.MBMessage",
				"MBMessage", "messageId");
			updateAssetEntry(
				"com.liferay.portlet.wiki.model.WikiPage", "WikiPage",
				"resourcePrimKey", "pageId");
		}
	}

	protected void updateAssetEntry(long classNameId, long classPK, String uuid)
		throws Exception {

		try (PreparedStatement ps = connection.prepareStatement(
				"update AssetEntry set classUuid = ? where classNameId = ? " +
					"and classPK = ?")) {

			ps.setString(1, uuid);
			ps.setLong(2, classNameId);
			ps.setLong(3, classPK);

			ps.executeUpdate();
		}
	}

	protected void updateAssetEntry(
			String className, String tableName, String columnName)
		throws Exception {

		long classNameId = PortalUtil.getClassNameId(className);

		StringBundler sb = new StringBundler(11);

		sb.append("update AssetEntry set classUuid = (select ");
		sb.append(tableName);
		sb.append(".uuid_ from ");
		sb.append(tableName);
		sb.append(" where ");
		sb.append(tableName);
		sb.append(".");
		sb.append(columnName);
		sb.append(" = AssetEntry.classPK) where (AssetEntry.classNameId = ");
		sb.append(classNameId);
		sb.append(StringPool.CLOSE_PARENTHESIS);

		runSQL(sb.toString());
	}

	protected void updateAssetEntry(
			String className, String tableName, String columnName1,
			String columnName2)
		throws Exception {

		try (LoggingTimer loggingTimer = new LoggingTimer(className)) {
			long classNameId = PortalUtil.getClassNameId(className);

			try (PreparedStatement ps = connection.prepareStatement(
					"select classPK from AssetEntry where classNameId = ?")) {

				ps.setLong(1, classNameId);

				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						long classPK = rs.getLong("classPK");

						String uuid = getUuid(
							tableName, columnName1, columnName2, classPK);

						updateAssetEntry(classNameId, classPK, uuid);
					}
				}
			}
		}
	}

}