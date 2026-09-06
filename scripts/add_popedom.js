const fs = require('fs');
const path = require('path');

function insertPopedom(file, annotation, importLine) {
  let t = fs.readFileSync(file, 'utf8');
  if (!t.includes('import cn.miyf.security.PopedomGroup')) {
    if (t.includes('import cn.miyf.security.MiyfPermission;')) {
      t = t.replace(
        'import cn.miyf.security.MiyfPermission;',
        'import cn.miyf.security.MiyfPermission;\n' + importLine,
      );
    } else if (t.includes('import cn.miyf.security.RequirePermission;')) {
      t = t.replace(
        'import cn.miyf.security.RequirePermission;',
        importLine + '\nimport cn.miyf.security.RequirePermission;',
      );
    } else {
      t = t.replace(/^(package .+;\r?\n)/m, '$1\n' + importLine + '\n');
    }
  }
  if (!t.includes('@PopedomGroup')) {
    t = t.replace(/(@Tag\([^)]*\)\s*\r?\n)(@RestController)/, `$1${annotation}\n$2`);
  }
  fs.writeFileSync(file, t, 'utf8');
  console.log('ok', path.basename(file));
}

const root =
  'e:\\ProgramData\\Document All\\xwechat_files\\wxid_eoj9x9r127ci22_bd90\\msg\\file\\2026-09\\miyf\\server';
const kitchenAnn =
  '@PopedomGroup(value = "11030000", name = "\u7ba1\u7406\u5458", product = "kitchen", sort = 10)';
const iamAnn =
  '@PopedomGroup(value = "10030000", name = "\u7ba1\u7406\u5458", product = "iam", sort = 5)';
const imp = 'import cn.miyf.security.PopedomGroup;';

const kitchenFiles = [
  'lib-app/kitchen-service/src/main/java/cn/miyf/kitchen/controller/admin/AdminDishController.java',
  'lib-app/kitchen-service/src/main/java/cn/miyf/kitchen/controller/admin/AdminCategoryController.java',
  'lib-app/kitchen-service/src/main/java/cn/miyf/kitchen/controller/admin/AdminRecipeController.java',
  'lib-app/kitchen-service/src/main/java/cn/miyf/kitchen/controller/admin/AdminCommentController.java',
  'lib-app/kitchen-service/src/main/java/cn/miyf/kitchen/controller/admin/AdminUserController.java',
  'lib-app/kitchen-service/src/main/java/cn/miyf/kitchen/controller/admin/AdminDashboardController.java',
  'lib-app/kitchen-service/src/main/java/cn/miyf/kitchen/controller/admin/AdminOperationLogController.java',
  'lib-app/kitchen-service/src/main/java/cn/miyf/kitchen/controller/FileUploadController.java',
];
const iamFiles = [
  'lib-auth/user-service/src/main/java/cn/miyf/controller/IamUserController.java',
  'lib-auth/permission-service/src/main/java/cn/miyf/controller/IamRoleController.java',
  'lib-auth/permission-service/src/main/java/cn/miyf/controller/IamPermissionController.java',
  'lib-auth/permission-service/src/main/java/cn/miyf/controller/IamPermGroupController.java',
  'lib-auth/organization-service/src/main/java/cn/miyf/controller/IamOrgUnitController.java',
  'lib-auth/permission-service/src/main/java/cn/miyf/controller/IamMenuController.java',
  'lib-auth/user-service/src/main/java/cn/miyf/controller/IamMeController.java',
];

for (const f of kitchenFiles) insertPopedom(path.join(root, f), kitchenAnn, imp);
for (const f of iamFiles) insertPopedom(path.join(root, f), iamAnn, imp);
