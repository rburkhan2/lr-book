package com.fingence.slayer.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;

import com.fingence.slayer.model.Asset;
import com.fingence.util.CellUtil;
import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Group;
import com.liferay.portal.security.auth.CompanyThreadLocal;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.asset.model.AssetEntry;
import com.liferay.portlet.asset.model.AssetVocabulary;
import com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetEntryLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetVocabularyLocalServiceUtil;

public class AssetHelper {

	public static long getVocabularyId(long userId, String title, ServiceContext serviceContext) {
		
		long guestGroupId = getGuestGroupId();
		
		long originalScopeGroupId = serviceContext.getScopeGroupId();
		serviceContext.setScopeGroupId(guestGroupId);
		
		AssetVocabulary assetVocabulary = null;
		try {
			assetVocabulary = AssetVocabularyLocalServiceUtil.getGroupVocabulary(guestGroupId, title);
		} catch (PortalException e) {
			e.printStackTrace();
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		if (Validator.isNull(assetVocabulary)) {
			try {
				assetVocabulary = AssetVocabularyLocalServiceUtil.addVocabulary(userId, title, serviceContext);
			} catch (PortalException e) {
				//e.printStackTrace();
			} catch (SystemException e) {
				//e.printStackTrace();
			}
		}
		
		serviceContext.setScopeGroupId(originalScopeGroupId);
		return assetVocabulary.getVocabularyId();
	}
	
	private static long getGuestGroupId() {
		
		long companyId = CompanyThreadLocal.getCompanyId();
		
		long guestGroupId = 0l;
		try {
			Group guestGroup = GroupLocalServiceUtil.getGroup(companyId, "Guest");
			guestGroupId = guestGroup.getGroupId();
		} catch (PortalException e) {
			e.printStackTrace();
		} catch (SystemException e) {
			e.printStackTrace();
		}
		return guestGroupId;
	}
	
	private static long getCategoryId(long userId, String name, ServiceContext serviceContext, long vocabularyId, long parentCategoryId) {
		
		if (Validator.isNull(name)) return 0l;
		
		long guestGroupId = getGuestGroupId();
		
		long originalScopeGroupId = serviceContext.getScopeGroupId();
		serviceContext.setScopeGroupId(guestGroupId);
		
		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(AssetCategory.class, PortalClassLoaderUtil.getClassLoader());
		dynamicQuery.add(RestrictionsFactoryUtil.eq("vocabularyId", vocabularyId));
		dynamicQuery.add(RestrictionsFactoryUtil.eq("name", name));
		dynamicQuery.add(RestrictionsFactoryUtil.eq("parentCategoryId", parentCategoryId));
		dynamicQuery.add(RestrictionsFactoryUtil.eq("userId", userId));
		dynamicQuery.add(RestrictionsFactoryUtil.eq("groupId", guestGroupId));
		
		AssetCategory assetCategory = null;
		
		try {
			@SuppressWarnings("unchecked")
			List<AssetCategory> assetCategories = AssetCategoryLocalServiceUtil.dynamicQuery(dynamicQuery);
			
			if (Validator.isNotNull(assetCategories) && !assetCategories.isEmpty()) {
				assetCategory = assetCategories.get(0);
			}
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		if (Validator.isNull(assetCategory)) {
			Map<Locale, String> titleMap = new HashMap<Locale, String>();
			titleMap.put(Locale.US, name);
			
			try {
				assetCategory = AssetCategoryLocalServiceUtil.addCategory(
						userId, parentCategoryId, titleMap, null, vocabularyId,
						null, serviceContext);
			} catch (PortalException e) {
				e.printStackTrace();
			} catch (SystemException e) {
				e.printStackTrace();
			}
			
			serviceContext.setScopeGroupId(originalScopeGroupId);			
		}
		
		return assetCategory.getPrimaryKey();
	}	
	
	public static void assignCategories(long assetId, long entryId, long userId, Row row,
			Map<String, Integer> columnNames, ServiceContext serviceContext,
			long bbSecurityVocabularyId, long bbIndustryVocabularyId) {
		
		String securityClass = CellUtil.getString(row.getCell(columnNames.get("BPIPE_REFERENCE_SECURITY_CLASS")));
		String securityTyp = CellUtil.getString(row.getCell(columnNames.get("SECURITY_TYP")));
		String securityTyp2 = CellUtil.getString(row.getCell(columnNames.get("SECURITY_TYP2")));
		
		if (securityClass.equalsIgnoreCase("FixedIncome")) {
			securityClass = "Fixed Income";
		}
		
		long securityClassId = getCategoryId(userId, securityClass, serviceContext, bbSecurityVocabularyId, 0l);
		long securityTypId = getCategoryId(userId, securityTyp, serviceContext, bbSecurityVocabularyId, securityClassId);
		
		if (Validator.isNotNull(securityTyp2) && !securityTyp2.equalsIgnoreCase(securityTyp)) {
			long securityTyp2Id = getCategoryId(userId, securityTyp2, serviceContext, bbSecurityVocabularyId, securityTypId);
		}
		
		if (securityTypId > 0l) {
			try {
				AssetCategoryLocalServiceUtil.addAssetEntryAssetCategory(entryId, securityTypId);
			} catch (SystemException e) {
				e.printStackTrace();
			}			
		}
		
		String industrySector = CellUtil.getString(row.getCell(columnNames.get("INDUSTRY_SECTOR")));
		String industryGroup =  CellUtil.getString(row.getCell(columnNames.get("INDUSTRY_GROUP")));
		String industrySubGroup = CellUtil.getString(row.getCell(columnNames.get("INDUSTRY_SUBGROUP")));
		
		long industrySectorId = getCategoryId(userId, industrySector, serviceContext, bbIndustryVocabularyId, 0l);
		long industryGroupId = getCategoryId(userId, industryGroup, serviceContext, bbIndustryVocabularyId, industrySectorId);
		long industrySubGroupId = getCategoryId(userId, industrySubGroup, serviceContext, bbIndustryVocabularyId, industryGroupId);
		
		if (industrySubGroupId > 0l) {
			try {
				AssetCategoryLocalServiceUtil.addAssetEntryAssetCategory(entryId, industrySubGroupId);
			} catch (SystemException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public static void assignBondCategory(long assetId, long entryId, long userId, Row row,
			Map<String, Integer> columnNames, ServiceContext serviceContext,
			long bondCPNVocabularyId) {
		
		String cpnType = CellUtil.getString(row.getCell(columnNames.get("CPN_TYP")));
		String mtyType = CellUtil.getString(row.getCell(columnNames.get("MTY_TYP")));
		
		long cpnTypeId = getCategoryId(userId, cpnType, serviceContext, bondCPNVocabularyId, 0l);
		long mtyTypeId = getCategoryId(userId, mtyType, serviceContext, bondCPNVocabularyId, cpnTypeId);
		
		if (mtyTypeId > 0l) {
			try {
				AssetCategoryLocalServiceUtil.addAssetEntryAssetCategory(entryId, mtyTypeId);
			} catch (SystemException e) {
				e.printStackTrace();
			}			
		}
	}	
	
	public static long updateAssetEntry(long assetId) {
		
		AssetEntry assetEntry = null;
		try {
			assetEntry = AssetEntryLocalServiceUtil.fetchEntry(Asset.class.getName(), assetId);
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		if (Validator.isNull(assetEntry)) {
			
			long entryId = 0l;
			try {
				entryId = CounterLocalServiceUtil.increment();
			} catch (SystemException e) {
				e.printStackTrace();
			}
			
			assetEntry = AssetEntryLocalServiceUtil.createAssetEntry(entryId);
			assetEntry.setClassName(Asset.class.getName());
			assetEntry.setClassPK(assetId);
			
			try {
				assetEntry = AssetEntryLocalServiceUtil.addAssetEntry(assetEntry);
			} catch (SystemException e) {
				e.printStackTrace();
			}	
		}
		
		return assetEntry.getEntryId();
	}	
}